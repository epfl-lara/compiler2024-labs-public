package alpine
package typing

import alpine.symbols
import alpine.symbols.Type

import scala.annotation.tailrec
import scala.collection.IndexedSeq
import scala.collection.mutable
import scala.Console.{RED, RESET, YELLOW}
import scala.util.Sorting

/** The solving of a constraint system.
 *
 * @param goals The goals to prove.
 * @param loggingIsEnabled `true` iff this instance should log a trace.
 * @param loggingIndentation The indentation prefixing logged messages.
 */
private[typing] final class Solver private (
    private val goals: mutable.ArrayBuffer[Constraint],
    val loggingIsEnabled: Boolean,
    val loggingIdentation: String = ""
):

  import Solver.Outcome.{Success, Failure, Product}

  /** The typer having initiated the elimination of proof obligations. */
  private var typer: Typer | Null = null

  /** A map from goal its outcome. */
  private val outcome = mutable.ArrayBuffer.fill(goals.length)(None: Option[Solver.Outcome])

  /** The fresh goals to solve. */
  private var fresh = (0 until goals.length).toList

  /** The goals currently state because the solver hasn't enough information to prove them. */
  private var stale = List[Int]()

  /** A map from variable to the goals that may help the solver deduce its substitution. */
  private var potentialInformationSources = mutable.HashMap[Type.Variable, List[Int]]()

  /** A map from variable to its deduced lower and upper bounds. */
  private var bounds = mutable.HashMap[Type.Variable, Solver.Bounds]()

  /** The root goals that could not be solved. */
  private var rootsOfFailure = List[Int]()

  /** A map from open type variable to its assignment.
   *
   *  This map is monotonically extended during constraint solving to assign a type to each open
   *  variable in the constraint system. A system is complete if it can be used to derive a
   *  complete substitution map w.r.t. its open type variables.
   */
  private var substitution = SubstitutionTable()

  /** A map from tree to its binding.
   *
   *  This map is monotonically extended during constraint solving to assign each name expression
   *  to its referred entity.
   */
  private var binding = mutable.HashMap[ast.Tree, symbols.EntityReference]()

  // Initialize the solver's internals.
  fresh.map(updatePotentialInformationSources)

  /** Eliminates the unsolved constraints of this instance and returns the best solution found,
   *  using `typer` to perform name lookup.
   */
  def solution(typer: Typer): Solver.Solution =
    this.typer = typer
    try solve() finally this.typer = null

  /** Eliminates the unsolved constraints of this instance and returns the best solution found. */
  @tailrec private def solve(): Solver.Solution =
    /** Sets the outcome of `g` and move to the next goal. */
    inline def continue(g: Int, o: Option[Solver.Outcome]): Solver.Solution =
      setOutcome(g, o)
      solve()

    fresh match
      case g :: gs =>
        fresh = gs
        goals(g) = goals(g).withTypeTransformed(substitution.walked)
        log(s"- ${YELLOW}solve${RESET}: ${goals(g)}")
        goals(g) match
          case c: Constraint.Equal =>
            continue(g, solveEqual(c, g))
          case c: Constraint.Subtype =>
            continue(g, solveSubtype(c, g))
          case c: Constraint.Apply =>
            continue(g, solveApply(c, g))
          case c: Constraint.Member =>
            continue(g, solveMember(c, g))
          case c: Constraint.Overload =>
            solveOverload(c, g)
      case Nil => formSolution()

  /** Returns the result of solving constraint `c`, whose identifier is `g`. */
  private def solveEqual(c: Constraint.Equal, g: Int): Option[Solver.Outcome] =
    if c.lhs.matches(c.rhs, unify) then Some(Success) else Some(failureToSolveEqual(c))

  /** Returns a failure to solve `c`. */
  private def failureToSolveEqual(c: Constraint.Equal): Solver.Outcome.Failure =
    Failure({ (m, _) =>
      val e = TypeError(
        s"'${m.reify(c.lhs)}' is not equal to '${m.reify(c.rhs)}'",
        c.origin.site)
      DiagnosticSet(e)
    })

  /** Returns the result of solving constraint `c`, whose identifier is `g`. */
  private def solveSubtype(c: Constraint.Subtype, g: Int): Option[Solver.Outcome] =
    import Solver.Bounds

    /** Handles cases where the RHS is a type variable. */
    def solveTypeBelowVariable(lhs: Type, rhs: Type.Variable): Option[Solver.Outcome] =
      bounds.get(rhs) match
        case Some(Bounds(l, h)) => bounds.put(rhs, Bounds(join(l, lhs), h))
        case None => bounds.put(rhs, Bounds(lhs, Type.Any))

      // Postpone iff there are no more fresh goals capable of refining our guess.
      if informationSources(rhs).exists(fresh.contains) then
        postpone(g)
      else
        // TODO: What about *nested* oxccurrences of `lhs` in stale constraints?
        val b = bounds(rhs)
        val s = List(
          schedule(Constraint.Equal(b.lower, rhs, c.origin)),
          schedule(Constraint.Subtype(b.lower, b.upper, c.origin)))
        Some(Product(s, makeDiagnoser(s)))

    /** Handles cases where the LHS is a type variable. */
    def solveVariableBelowType(lhs: Type.Variable, rhs: Type): Option[Solver.Outcome] =
      bounds.get(lhs) match
        case Some(Bounds(l, h)) => bounds.put(lhs, Bounds(l, meet(h, rhs)))
        case None => bounds.put(lhs, Bounds(Type.Never, rhs))

      // Postpone iff there are no more fresh goals capable of refining our guess.
      if informationSources(lhs).exists(fresh.contains) then
        postpone(g)
      else
        // TODO: What about *nested* occurrences of `rhs` in stale constraints?
        val b = bounds(lhs)
        val s = List(
          schedule(Constraint.Equal(lhs, b.upper, c.origin)),
          schedule(Constraint.Subtype(b.lower, b.upper, c.origin)))
        Some(Product(s, makeDiagnoser(s)))

    /** Handles cases where the LHS is a record and the RHS is a sum. */
    def solveRecordBelowSum(lhs: Type.Record, rhs: Type.Sum): Option[Solver.Outcome] =
      @tailrec def loop(t: Type.Record, u: List[Type.Record]): Boolean =
        u match
          case r :: rs =>
            if t.structurallyPrecedes(r) then
              false
            else if t.structurallyMatches(r) then
              t.matches(r, unify)
            else
              loop(t, rs)
          case Nil => false

      // The LHS must match one element of the RHS.
      if loop(lhs, rhs.members) then Some(Success) else Some(failureToSolveSubtype(c))

    /** Handles cases where the LHS and RHS are sums. */
    def solveSumBelowSum(lhs: Type.Sum, rhs: Type.Sum): Option[Solver.Outcome] =
      @tailrec def loop(t: List[Type.Record], u: List[Type.Record]): Boolean =
        t match
          case l :: ls => u match
            case r :: rs =>
              if l.structurallyPrecedes(r) then
                false
              else if l.structurallyMatches(r) then
                if l.matches(r, unify) then loop(ls, rs) else false
              else
                loop(t, rs)
            case Nil => false
          case Nil => true

      // Each element if the LHS must match one element of the RHS.
      if loop(lhs.members, rhs.members) then Some(Success) else Some(failureToSolveSubtype(c))

    // Trivial if `lhs` is equal to `rhs`.
    if c.lhs == c.rhs then Some(Success) else c.rhs match
      // Upper bound `Any`.
      case Type.Any =>
        Some(Success)

      // Upper bound is a type variable.
      case u: Type.Variable =>
        solveTypeBelowVariable(c.lhs, u)

      // Upper bound is a sum.
      case u: Type.Sum => c.lhs match
        case t: Type.Variable =>
          solveVariableBelowType(t, c.rhs)
        case t: Type.Record =>
          solveRecordBelowSum(t, u)
        case t: Type.Sum =>
          solveSumBelowSum(t, u)
        case _ =>
          Some(failureToSolveSubtype(c))

      // Upper bound is anything else.
      case u => c.lhs match
        case Type.Never =>
          Some(Success)
        case t =>
          if t.matches(u, unify) then Some(Success) else Some(failureToSolveSubtype(c))

  /** Returns a failure to solve `c`. */
  private def failureToSolveSubtype(c: Constraint.Subtype): Solver.Outcome.Failure =
    Failure({ (m, _) =>
      val e = TypeError(
        s"'${m.reify(c.lhs)}' is not subtype of '${m.reify(c.rhs)}'",
        c.origin.site)
      DiagnosticSet(e)
    })

  /** Returns the result of solving constraint `c`, whose identifier is `g`. */
  private def solveApply(c: Constraint.Apply, g: Int): Option[Solver.Outcome] =
    c.function match
      case _: Type.Variable =>
        postpone(g)
      case f: Type.Arrow =>
        simplifyApply(c, f, g)
      case _ =>
        Some(invalidCallee(c))

  private def simplifyApply(c: Constraint.Apply, f: Type.Arrow, g: Int): Option[Solver.Outcome] =
    if !f.labels.iterator.sameElements(c.labels) then
      Some(invalidInputLabels(c, f))
    else
      var s = List[Int]()
      val o = c.origin.subordinate
      for (a, b) <- c.inputs.zip(f.inputs) do
        s = s.prepended(schedule(Constraint.Subtype(a.value, b.value, o)))
      s = s.prepended(schedule(Constraint.Equal(c.output, f.output, o)))
      Some(Product(s, makeDiagnoser(s)))

  /** Returns a failure to solve `c` because its callee isn't an arrow. */
  private def invalidCallee(c: Constraint.Apply): Solver.Outcome.Failure =
    Failure({ (m, _) =>
      val e = TypeError(
        s"cannot apply value of non-arrow type '${m.reify(c.function)}'",
        c.origin.site)
      DiagnosticSet(e)
    })

  /** Returns a failure to solve `c` because its callee `f` isn't applied with the right labels. */
  private def invalidInputLabels(c: Constraint.Apply, f: Type.Arrow): Solver.Outcome.Failure =
    Failure({ (m, _) =>
      val expected = "(" + f.labels.map(_.getOrElse("_")).mkString(", ") + ")"
      val found = "(" + c.labels.map(_.getOrElse("_")).mkString(", ") + ")"
      val e = TypeError(
        s"invalid argument labels; expected '${expected}' but found '${found}'",
        c.origin.site)
      DiagnosticSet(e)
    })

  /** Returns the result of solving constraint `c`, whose identifier is `g`. */
  private def solveMember(c: Constraint.Member, g: Int): Option[Solver.Outcome] =
    substitution.reify(c.lhs, (v) => v) match
      case _: Type.Variable =>
        postpone(g)
      case q: Type.Meta if q.instance.isVariable =>
        postpone(g)
      case q => typer.nn.lookupMember(c.member, q) match
        case Nil =>
          Some(failureToSolveMember(c))
        case pick :: Nil =>
          binding.put(c.selection, symbols.EntityReference(pick, c.rhs))
          val s = List(schedule(
            Constraint.Equal(pick.tpe, c.rhs, c.origin.subordinate)))
          Some(Product(s, makeDiagnoser(s)))
        case picks =>
          val choices = picks.map((e) => symbols.EntityReference(e, e.tpe))
          val s = List(schedule(
            Constraint.Overload(c.selection, choices, c.rhs, c.origin.subordinate)))
          Some(Product(s, makeDiagnoser(s)))

  /** Returns a failure to solve `c`. */
  private def failureToSolveMember(c: Constraint.Member): Solver.Outcome.Failure =
    Failure({ (m, _) =>
      val e = TypeError(
        s"'${c.lhs}' has no member '${c.member}'",
        c.origin.site)
      DiagnosticSet(e)
    })

  /** Returns the best solution for solving the remaining goals with one of the choices in `c`,
   *  whose identifier is `g`.
   */
  private def solveOverload(c: Constraint.Overload, g: Int): Solver.Solution =
    var best = List[Solver.Solution]()

    for pick <- c.candidates do
      log(s"  - fork ${pick}")
      val child = duplicate(loggingIdentation + " " * 2)
      val e = List(child.schedule(Constraint.Equal(pick.tpe, c.tpe, c.origin)))
      child.setOutcome(g, Some(Product(e, makeDiagnoser(e))))
      child.binding.put(c.name, pick)
      val newSolution = child.solution(typer)

      if best.isEmpty || (newSolution.score < best.head.score) then
        best = List(newSolution)
      else if newSolution.score == best.head.score then
        best = best.prepended(newSolution)

    if best.length == 1 then best.head else failureToSolveOverload(c, best)

  /** Returns a failure to solve `c` due to ambigous `solutions`. */
  private def failureToSolveOverload(
      c: Constraint.Overload, solutions: List[Solver.Solution]
  ): Solver.Solution =
    Solver.Solution(
      solutions.head.substitution,
      solutions.head.binding,
      solutions.head.diagnostics.inserting(
        TypeError(s"ambiguous use of overloaded identifier", c.origin.site)))

  /** Returns a diagnoser producing the diagnostics of all failed goals in `subordinates.` */
  private def makeDiagnoser(subordinates: List[Int]): Solver.FailureDiagnoser =
    (m, o) =>
      var partialResult = DiagnosticSet()
      for g <- subordinates do
        partialResult = o(g) match
          case Some(Solver.Outcome.Failure(d)) => partialResult.union(d(m, o))
          case _ => partialResult
      partialResult

  /** Returns `true` iff `lhs` and `rhs` can be unified, updating the type substitution table. */
  private def unify(lhs: Type, rhs: Type): Boolean =
    (substitution.walked(lhs), substitution.walked(rhs)) match
      case (t: Type.Variable, u) => assume(t, u); true
      case (t, u: Type.Variable) => assume(u, t); true
      case (t, u) => t == u

  /** Extends the type substution table to map `t` to `u`. */
  private def assume(t: Type.Variable, u: Type): Unit =
    substitution.put(t, u)
    refresh()

  /** Refresh stale constraints containing variables having been assigned. */
  private def refresh(): Unit =
    var s = List[Int]()
    for g <- stale do
      var changed = false
      goals(g) = goals(g).withTypeTransformed({ (t) =>
        val u = substitution.reify(t, (v) => v)
        changed = changed || (t != u)
        u
      })
      if changed then
        fresh = fresh.prepended(g)
      else
        s = s.prepended(g)
    stale = s

  /** Schedules `g` to be solved only once the solver has inferred more information about at least
   *  one of its type variables. */
  private def postpone(g: Int): Option[Solver.Outcome] =
    stale = stale.prepended(g)
    None

  /** Schedules `c` to be solved in the future and returns its identity. */
  private def schedule(c: Constraint): Int =
    val newIdentity = goals.length
    goals.append(c)
    outcome.append(None)
    fresh = fresh.prepended(newIdentity)
    updatePotentialInformationSources(newIdentity)
    newIdentity

  /** Records the outcome `o` for the goal `g`. */
  private def setOutcome(g: Int, o: Option[Solver.Outcome]): Unit =
    log(s"  ${describe(o)}")
    assert(outcome(g).isEmpty)
    outcome(g) = o

  /** Returns the goals that can be used to deduce the substitution of `v`. */
  private def informationSources(v: Type.Variable): List[Int] =
    potentialInformationSources.get(v)
      .map((s) => s.filter((g) => (outcome(g) == None) && goals(g).variables.contains(v)))
      .getOrElse(List())

  private def updatePotentialInformationSources(g: Int): Unit =
    for v <- goals(g).variables do
      potentialInformationSources.updateWith(v)((s) => s.map(_.prepended(g)).orElse(Some(List(g))))

  /** Returns `true` iff the goal `g` failed and isn't subordinate of any other goal. */
  private def isRootOfFailure(g: Int): Boolean =
    (goals(g).origin.parent == None) && failed(g)

  /** Returns `true` iff the goal `g` failed.
   *
   *  Important: `g` not having failed doesn't mean `g` has succeeded!
   */
  private def failed(g: Int): Boolean =
    outcome(g) match
      case Some(Failure(_)) => true
      case Some(Product(s, _)) => s.exists(failed)
      case _ => false

  /** Returns the most general type that is subtype of both `a` and `b`. */
  private def meet(a: Type, b: Type): Type =
    def impl(a: Type, b: Type, reverse: Boolean): Type =
      val u = substitution.reify(b, (v) => v)
      substitution.reify(a, (v) => v) match
        case Type.Any => u
        case t if t == u => u

        case t: Type.Record => u match
          case v: Type.Sum =>
            if v.members.contains(t) then t else Type.Never
          case _ =>
            Type.Never

        case t: Type.Sum => u match
          case v: Type.Sum =>
            t.intersect(v)
          case _ =>
            if reverse then impl(u, t, false) else Type.Never

        case t => if reverse then impl(u, t, false) else Type.Never

    // Note: `impl` calls itself with `a` and `b` swapped once if `reversed` is `true` so that we
    // can write all patterns in terms of the LHS.
    impl(a, b, true)

  /** Returns the least general type that is supertype of both `a` and `b`. */
  private def join(a: Type, b: Type): Type =
    def impl(a: Type, b: Type, reverse: Boolean): Type =
      val u = substitution.reify(b, (v) => v)
      substitution.reify(a, (v) => v) match
        case Type.Never => u
        case t if t == u => u

        case t: Type.Record => u match
          case v: Type.Record =>
            Type.Sum.fromPair(t, v).getOrElse(Type.Any)
          case v: Type.Sum =>
            v.inserting(t).getOrElse(Type.Any)
          case _ =>
            Type.Any

        case t: Type.Sum => u match
          case v: Type.Sum =>
            t.union(v).getOrElse(Type.Any)
          case _ =>
            if reverse then impl(u, t, false) else Type.Any

        case t => if reverse then impl(u, t, false) else Type.Any

    // Note: `impl` calls itself with `a` and `b` swapped once if `reversed` is `true` so that we
    // can write all patterns in terms of the LHS.
    impl(a, b, true)

  /** Returns a clone of this solver. */
  private def duplicate(loggingIdentation: String): Solver =
    val copy = new Solver(mutable.ArrayBuffer(), loggingIsEnabled, loggingIdentation)
    copy.goals.appendAll(goals)
    copy.outcome.appendAll(outcome)
    copy.fresh = fresh
    copy.stale = stale
    copy.potentialInformationSources = potentialInformationSources.clone()
    copy.bounds = bounds.clone()
    copy.rootsOfFailure = rootsOfFailure
    copy.substitution = substitution.optimized
    copy.binding = binding.clone()
    copy

  /** Creates and returns a solution from the current state of the solver. */
  private def formSolution(): Solver.Solution =
    for g <- stale do
      val c = goals(g)
      val e: Solver.FailureDiagnoser = (m, _) =>
        val walked = c.withTypeTransformed(m.walked)
        DiagnosticSet(TypeError(s"cannot solve ${walked}", c.origin.site))
      setOutcome(g, Some(Failure(e)))

    assert(outcome.forall(_.isDefined))

    val s = substitution.optimized
    var d = DiagnosticSet()
    for (o, i) <- outcome.zipWithIndex if isRootOfFailure(i) do
      d = d.union(o.get.diagnose(s, outcome))

    log(s"- ${YELLOW}done${RESET}")
    Solver.Solution(s, binding.toMap, d)

  /** Logs `line` in the standard output iff `loggingIsEnabled` is `true`. */
  private def log(line: => String): Unit =
    if loggingIsEnabled then println(loggingIdentation + line)

  /** Returns a textual description of outcome `o`. */
  private def describe(o: Option[Solver.Outcome]): String =
    o match
      case Some(Success) =>
        "succeeded"
      case Some(Failure(d)) =>
        s"${RED}failed${RESET}: " + d(substitution, outcome).elements.map(_.summary).mkString("; ")
      case Some(Product(s, _)) =>
        "simplified: " + s.map(goals.apply).mkString("; ")
      case None =>
        "postponed"

end Solver

private[typing] object Solver:

  /** Creates an instance for solving `obligations`. */
  def apply(obligations: ProofObligations, loggingIsEnabled: Boolean = false): Solver =
    val goals = obligations.constraints.to(mutable.ArrayBuffer)
    new Solver(goals, loggingIsEnabled)

  /** A closure producing the diagnostics of the failure to solve a goal.
   *
   *  The first parameter is the substitution table established by the solver and the second is a
   *  map from goal to its outcome.
   */
  type FailureDiagnoser = (SubstitutionTable, IndexedSeq[Option[Outcome]]) => DiagnosticSet

  /** The known bounds of a type variable. */
  final case class Bounds(lower: Type , upper: Type):

    override def toString: String =
      s"(${lower}) <: (${upper})"

  end Bounds

  /** The outcome of a goal. */
  enum Outcome:

    /** The goal was solved; information deduced from it has been stored in the solver's tate */
    case Success extends Outcome

    /** The goal was unsatisfiable.
     *
     *  The goal was in conflict with the assumptions held by the solver. The payload is a closure
     *  generating a diagnostic of the conflict.
     */
    case Failure(diagnostics: FailureDiagnoser) extends Outcome

    /** The goal was broken into subordinate goals.
     *
     *  The payload is a non-empty list of subordinate goals along with a closure generating a
     *  a diagnostic in case one of the subordinate goals are unsatisfiable.
     */
    case Product(subordinates: List[Int], diagnostics: FailureDiagnoser) extends Outcome

    /** Returns the diagnostics constructor of this outcome. */
    def diagnose(s: SubstitutionTable, o: IndexedSeq[Option[Outcome]]): DiagnosticSet =
      this match
        case Outcome.Success => DiagnosticSet()
        case Outcome.Failure(d) => d(s, o)
        case Outcome.Product(_, d) => d(s, o)

  end Outcome

  /** A solution returned by a constraint solver.
  *
  *  @param substitution A map from open type variable to its assignment.
  *  @param binding A map from tree to its binding.
  *  @param diagnostics The diagnostics associated with the solution.
  */
  private[typing] final class Solution(
      val substitution: SubstitutionTable,
      val binding: Map[ast.Tree, symbols.EntityReference],
      val diagnostics: DiagnosticSet
  ):

    /** Creates an empty solution. */
    def this() =
      this(SubstitutionTable(), Map(), DiagnosticSet())

    /** `true` iff the solution has no error. */
    def isSound: Boolean =
      !diagnostics.containsError

    /** A value for ranking different solutions to the same constraints (least is best). */
    def score: Int =
      diagnostics.elements.size

  end Solution

end Solver
