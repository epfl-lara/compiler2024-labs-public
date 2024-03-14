package alpine
package typing

import alpine.ast
import alpine.symbols
import alpine.symbols.{Entity, Type}
import alpine.typing.Typer.Context
import alpine.util.{Memo, FatalError}

import scala.annotation.tailrec
import scala.collection.mutable

// Visiting a declaration == type checking it
// Visiting an expression == type inference
//    When the "wake" ends, solving proof constraints performs type checking
// Unchecked type break recursion cycles

/** The construction and verification of a program's type information. */
final class Typer(
    val loggingIsEnabled: Boolean = false
) extends ast.TreeVisitor[Typer.Context, Type]:

  /** The program being typed.
   *
   *  This property is set by `check`, which is the entry point of the algorithm.
   */
  private[typing] var syntax: alpine.Program | Null = null

  /** The next fresh anonymous scope identifier. */
  private var nextFreshScopeIdentifier = 0

  /** The next fresh type variable identifier. */
  private var nextFreshVariableIdentifier = 0

  /** The information computed by the typer. */
  private var properties = Typer.Properties()

  /** The diagnostics collected by the typer. */
  private var _diagnostics = DiagnosticSet()

  /** The diagnostics collected by the typer. */
  def diagnostics: DiagnosticSet =
    _diagnostics

  /** Constructs and verifies the type information of `p`. */
  def check(p: alpine.Program): TypedProgram =
    given Typer.Context = Typer.Context()
    this.syntax = p
    try p.declarations.foreach(_.visit(this)) finally this.syntax = null
    TypedProgram(
      untyped = p,
      treeToType = properties.checkedType.toMap,
      scopeToName = properties.scopeToName.toMap,
      declarationToScope = properties.declarationToScope.toMap,
      declarationToNameDeclared = properties.declarationToNameDeclared.toMap,
      treeToReferredEntity = properties.treeToReferredEntity.toMap)

  // --- Type checking --------------------------------------------------------

  def visitLabeled[T <: ast.Tree](n: ast.Labeled[T])(using context: Typer.Context): Type =
    unexpectedVisit(n)

  def visitBinding(d: ast.Binding)(using context: Typer.Context): Type =
    addToParent(d)
    assignNameDeclared(d)

    val t = context.ignoringDuringLookup(d, { (inner) =>
      given Typer.Context = inner
      d.ascription match
        case Some(a) =>
          val required = evaluateTypeTree(a)
          d.initializer.map((i) => checkInstanceOf(i, required))
          properties.checkedType.put(d, required)
          required

        case _ => d.initializer match
          case Some(i) =>
            val inferred = checkedType(i)
            properties.checkedType.put(d, inferred)
            inferred
          case _ =>
            // We can only get here if `d` is used as the pattern of a match.
            context.obligations.constrain(d, freshTypeVariable())
    })
    memoizedUncheckedType(d, (_) => t)

  def visitFunction(d: ast.Function)(using context: Typer.Context): Type =
    if !d.genericParameters.isEmpty then
      throw FatalError("unsupported generic parameters", d.genericParameters.head.site)

    addToParent(d)
    assignScopeName(d)
    assignNameDeclared(d)

    val t: Type = context.inScope(d, { (inner) =>
      ???
    })

    val result = if t(Type.Flags.HasError) then Type.Error else t
    properties.checkedType.put(d, result)
    result
  def visitParameter(d: ast.Parameter)(using context: Typer.Context): Type =
    addToParent(d)
    assignNameDeclared(d)

    val result = d.ascription match
      case Some(a) =>
        evaluateTypeTree(a)(using context)
      case _ =>
        report(TypeError(s"missing parameter type annotation", d.site))
        Type.Error

    properties.checkedType.put(d, result)
    result

  def visitIdentifier(e: ast.Identifier)(using context: Typer.Context): Type =
    bindEntityReference(e, resolveUnqualifiedTermIdentifier(e.value, e.site))

  def visitBooleanLiteral(e: ast.BooleanLiteral)(using context: Typer.Context): Type =
    context.obligations.constrain(e, Type.Bool)

  def visitIntegerLiteral(e: ast.IntegerLiteral)(using context: Typer.Context): Type =
    context.obligations.constrain(e, Type.Int)

  def visitFloatLiteral(e: ast.FloatLiteral)(using context: Typer.Context): Type =
    context.obligations.constrain(e, Type.Float)

  def visitStringLiteral(e: ast.StringLiteral)(using context: Typer.Context): Type =
    context.obligations.constrain(e, Type.String)

  def visitRecord(e: ast.Record)(using context: Typer.Context): Type =
    ???

  def visitSelection(e: ast.Selection)(using context: Typer.Context): Type =
    val q = e.qualification.visit(this)
    val m = freshTypeVariable()
    
    e.selectee match
      case s: ast.Identifier =>
        ???
      case s: ast.IntegerLiteral =>
        ???
    context.obligations.constrain(e, m)

  def visitApplication(e: ast.Application)(using context: Typer.Context): Type =
    ???

  def visitPrefixApplication(e: ast.PrefixApplication)(using context: Typer.Context): Type =
    ???

  def visitInfixApplication(e: ast.InfixApplication)(using context: Typer.Context): Type =
    ???

  def visitConditional(e: ast.Conditional)(using context: Typer.Context): Type =
    ???

  def visitMatch(e: ast.Match)(using context: Typer.Context): Type =
    // Scrutinee is checked in isolation.
    val scrutinee = checkedType(e.scrutinee)

    // Patterns are subtype of the scrutinee; bodies are subtype of the expression.
    val merge = freshTypeVariable()
    val patterns = e.cases.map({ (c) =>
      assignScopeName(c)
      val p = c.pattern.visit(this)
      context.obligations.add(Constraint.Subtype(p, scrutinee, Constraint.Origin(c.pattern.site)))
      val e = context.inScope(c, (inner) => c.body.visit(this)(using inner))
      context.obligations.add(Constraint.Subtype(e, merge, Constraint.Origin(c.body.site)))
    })
    
    context.obligations.constrain(e, merge)

  def visitMatchCase(e: ast.Match.Case)(using context: Typer.Context): Type =
    unexpectedVisit(e)

  def visitLet(e: ast.Let)(using context: Typer.Context): Type =
    ???

  def visitLambda(e: ast.Lambda)(using context: Typer.Context): Type =
    ???

  def visitParenthesizedExpression(
      e: ast.ParenthesizedExpression
  )(using context: Typer.Context): Type =
    ???

  def visitAscribedExpression(e: ast.AscribedExpression)(using context: Typer.Context): Type =
    val result = evaluateTypeTree(e.ascription) match
      case Type.Error =>
        Type.Error
      case ascription =>
        ???
    context.obligations.constrain(e, result)

  def visitTypeIdentifier(e: ast.TypeIdentifier)(using context: Typer.Context): Type =
      ???

  def visitRecordType(e: ast.RecordType)(using context: Typer.Context): Type =
    ???

  def visitTypeApplication(e: ast.TypeApplication)(using context: Typer.Context): Type =
    throw FatalError("unsupported generic parameters", e.site)

  def visitArrow(e: ast.Arrow)(using context: Typer.Context): Type =
    ???


  def visitSum(e: ast.Sum)(using context: Typer.Context): Type =
    var hasErrorMember = false
    var partialResult = Type.Sum.empty
    for m <- e.members do
      evaluateTypeTree(m) match
        case t: Type.Record =>
          if !hasErrorMember then partialResult.inserting(t) match
            case Some(s) =>
              partialResult = s
            case _ =>
              report(TypeError("sum type can only contain structurally distinct members", m.site))
              hasErrorMember = true

        case Type.Error =>
          // Error already reported.
          hasErrorMember = true

        case u =>
          report(TypeError("sum type members must be record types", m.site))
          hasErrorMember = true

    if hasErrorMember then Type.Error else partialResult

  def visitParenthesizedType(e: ast.ParenthesizedType)(using context: Typer.Context): Type =
    ???

  def visitValuePattern(p: ast.ValuePattern)(using context: Typer.Context): Type =
    context.obligations.constrain(p, p.value.visit(this))

  def visitRecordPattern(p: ast.RecordPattern)(using context: Typer.Context): Type =
    ???

  def visitWildcard(p: ast.Wildcard)(using context: Typer.Context): Type =
    ???

  def visitTypeDeclaration(e: ast.TypeDeclaration)(using context: Typer.Context): Type =
    report(TypeError("type declarations are not supported", e.site))
    Type.Error // Type declarations are not supported.

  def visitError(n: ast.ErrorTree)(using context: Typer.Context): Type =
    unexpectedVisit(n)

  /** Returns the unchecked type of `d`, computing it if necessary. */
  private def uncheckedType(d: ast.Declaration)(using context: Typer.Context): Type =
    cachedUncheckedType(d).getOrElse(d.visit(this))

  /** Returns the unchecked type of `d` if it's in cache. */
  private def cachedUncheckedType(d: ast.Declaration): Option[Type] =
    properties.uncheckedType.get(d).flatMap({
      case Memo.InProgres => None
      case Memo.Computed(cached) => Some(cached)
    })

  /** Returns the unchecked type of `d`, computing it with `compute` if it's not in cache. */
  private def memoizedUncheckedType[T <: ast.Declaration](d: T, compute: T => Type): Type =
    properties.uncheckedType.get(d) match
      case Some(Memo.InProgres) => throw FatalError("infinite recursion detected", d.site)
      case Some(Memo.Computed(cached)) => cached
      case _ =>
        val t = compute(d)
        properties.uncheckedType.put(d, Memo.Computed(t))
        t

  /** Computes and returns the unchecked type of `d`. */
  private def computedUncheckedType(d: ast.Function)(using context: Typer.Context): Type.Arrow =
    val inputs = computedUncheckedInputTypes(d.inputs)
    val output = d.output.map(evaluateTypeTree).getOrElse(Type.Unit)
    Type.Arrow(inputs, output)

  /** Computes and returns the unchecked types of the inputs `ps`. */
  private def computedUncheckedInputTypes(
      ps: Iterable[ast.Parameter]
  )(using context: Typer.Context): List[Type.Labeled] =
    val partialResult = mutable.ListBuffer[symbols.Type.Labeled]()
    for p <- ps do
      partialResult += Type.Labeled(p.label, uncheckedType(p))
    partialResult.toList

  /** Checks that `e` has type `t`. */
  private def checkInstanceOf(e: ast.Expression, t: Type)(using context: Typer.Context): Unit =
    val c = context.withNewProofObligations
    val u = partiallyCheckedType(e, c)
    c.obligations.add(Constraint.Subtype(u, t, Constraint.Origin(e.site)))
    discharge(c.obligations, e)

  /** Returns the checked type of `e`, reporting an error if it is provably not convertible to
   *  `subtype` (i.e., `subtype` isn't subtype of the result).
   */
  private def checkedTypeEnsuringConvertible(
      e: ast.Expression, subtype: Type
  )(using context: Typer.Context): Type =
    val s = checkedType(e)
    if !subtype.isSubtypeOf(s) then
      report(TypeError(s"conversion from '${s}' to '${subtype}' always fails", e.site))
    s

  /** Returns the checked type of `e`. */
  private def checkedType(e: ast.Expression)(using context: Typer.Context): Type =
    val c = context.withNewProofObligations
    val u = partiallyCheckedType(e, c)
    val s = discharge(c.obligations, e)
    s.substitution.reify(u)

  /** Returns the partially checked type of `e` in the given `context`. */
  private def partiallyCheckedType(e: ast.Expression, context: Typer.Context): Type =
    assert(context.obligations.isEmpty)
    e.visit(this)(using context)

  /** Proves the formulae in `obligations`, which relate to the well-typedness of `n`, returning
   *  the best assignment of universally quantified variables.
   */
  private def discharge(obligations: ProofObligations, n: ast.Tree): Solver.Solution =
    // Error already reported if the proof obligations are not satisfiable.
    val result = if !obligations.isUnsatisfiable then
      val solver = Solver(obligations, loggingIsEnabled)
      val s = solver.solution(this)
      commit(s, obligations)
      s
    else
      assert(!diagnostics.isEmpty)
      Solver.Solution()

    obligations.clear()
    result

  /** Commits to the choices made in `solution` to satisfy `obligations`. */
  private def commit(solution: Solver.Solution, obligations: ProofObligations): Unit =
    for (n, t) <- obligations.inferredType do
      val u = solution.substitution.reify(t)
      val v = properties.checkedType.put(n, u)
      assert(v.map((x) => x == u).getOrElse(true))

      // The cache may have an unchecked type for `n` if it's a declaration whose type has been
      // inferred (i.e., variable in a match case without ascription).
      properties.uncheckedType.updateWith(n)((x) => x.map((_) => Memo.Computed(u)))

    for (n, r) <- solution.binding do
      val s = symbols.EntityReference(r.entity, solution.substitution.reify(r.tpe))
      properties.treeToReferredEntity.put(n, s)

    reportBatch(solution.diagnostics.elements)
    assert(solution.isSound || diagnostics.containsError, "inference failed without diagnostic")

  // --- Compile-time evaluation ----------------------------------------------

  /** Evaluates a record field index. */
  private def evaluateFieldIndex(s: ast.IntegerLiteral): Option[Int] =
    try
      Some(s.value.toInt)
    catch _ =>
      report(TypeError(s"'${s.value}' is not a valid record field index", s.site))
      None

  /** Evaluates the type-level expression `e`. */
  private def evaluateTypeTree(e: ast.Type)(using context: Typer.Context): Type =
    val t = e.visit(this)
    properties.checkedType.put(e, t)
    t

  // --- Name resolution ------------------------------------------------------

  /** Returns the type of the entity referred to by `identifier` without qualification in the
   *  current scope, along with associated constraints.
   *
   *  A diagnostic is reported at `diagnosticStie` if `identifier` is undefined or if it refers to
   *  type-level entities.
   */
  private def resolveUnqualifiedTermIdentifier(
      identifier: String, diagnosticSite: SourceSpan
  )(using context: Typer.Context): List[symbols.EntityReference] =
    // The set of all looked up entities .
    val allEntities = lookupUnqualified(identifier)
    // The subset of looked up term-level entities that don't have an error type.
    val eligibleEntities = allEntities.filter((e) => e.tpe != Type.Error)

    if eligibleEntities.isEmpty then
      // No eligible candidate; use the set of all entities to diagnose the error.
      if allEntities.isEmpty then
        report(TypeError(s"undefined identifier '${identifier}'", diagnosticSite))
      else
        report(TypeError(s"ambiguous use of '${identifier}'", diagnosticSite))

    eligibleEntities.map((e) => symbols.EntityReference(e, e.tpe))

  /** Constrains `e` to be bound to one of `candidates`, returning its type.
   *
   *  The method returns `Type.Error` without reporting any diagnostic if there is no candidate.
   *  Otherwise, it creates the necessary proof obligations to select the right candidate.
   *
   *  @param e An identifier or operator.
   *  @param candidates The set of entities to which `e` can possibly refer.
   */
  private def bindEntityReference(
      e: ast.Tree, candidates: List[symbols.EntityReference]
  )(using context: Typer.Context): Type =
    candidates match
      case Nil =>
        context.obligations.constrain(e, Type.Error)
      case pick :: Nil =>
        properties.treeToReferredEntity.put(e, pick)
        context.obligations.constrain(e, pick.tpe)
      case picks =>
        val t = freshTypeVariable()
        context.obligations.add(Constraint.Overload(e, picks, t, Constraint.Origin(e.site)))
        context.obligations.constrain(e, t)

  /** Returns the entities possibly referred to by `identifier` occurring with `qualification`. */
  private[typing] def lookupMember(
      identifier: String | Int, qualification: symbols.Type
  ): List[Entity] =
    qualification match
      case t: Type.Record =>
        lookupRecordMember(identifier, t).toList
      case t: Type.Meta =>
        lookupMethod(identifier, t.instance).toList
      case _ =>
        List()

  /** Returns the entity referred to by `identifier` occurring with `qualification`. */
  private def lookupRecordMember(
      identifier: String | Int, qualification: symbols.Type.Record
  ): Option[Entity] =
    identifier match
      case s: String =>
        qualification.fields.indexWhere((f) => f.label.contains(s)) match
          case -1 => None
          case i => Some(Entity.Field(qualification, i))
      case i: Int if i < qualification.fields.length =>
        Some(Entity.Field(qualification, i))
      case _ =>
        None

  /** Returns the entities possibly referred to by `identifier` occurring with `qualification`. */
  private def lookupMethod(identifier: String | Int, qualification: symbols.Type): List[Entity] =
    List()

  /** Returns the entities possibly referred to by `identifier` without qualification in the
   *  current scope.
   */
  private def lookupUnqualified(identifier: String)(using context: Typer.Context): List[Entity] =
    @tailrec def loop(s: List[ast.Tree]): List[Entity] =
      s match
        case (e: ast.Function) :: outer =>
          val r = e.inputs.collect({
            case p if (p.identifier == identifier) => entityDeclared(p)
          })
          if !r.isEmpty then r else loop(outer)

        case (e: ast.Lambda) :: outer =>
          val r = e.inputs.collect({
            case p if (p.identifier == identifier) => entityDeclared(p)
          })
          if !r.isEmpty then r else loop(outer)

        case (e: ast.Let) :: outer =>
          if !context.isIgnoredByLookup(e.binding) && (e.binding.identifier == identifier) then
            List(entityDeclared(e.binding))
          else
            loop(outer)

        case (e: ast.Match.Case) :: outer =>
          val r = e.pattern.declarationsWithPath.collect({
            case (d, _) if d.identifier == identifier => entityDeclared(d)
          })
          if !r.isEmpty then r else loop(outer)

        case _ :: outer =>
          loop(outer)

        case Nil =>
          lookupTopLevel(symbols.Name(None, identifier))

    loop(context.scopes)

  /** Returns the entities possibly referred to by `name` at the top level scope. */
  private def lookupTopLevel(name: symbols.Name)(using context: Typer.Context): List[Entity] =
    var partialResult = List[Entity]()
    for d <- syntax.nn.declarations if !context.declarationsIgnoredByLookup.contains(d) do
      if nameDeclared(d) == name then
        partialResult = partialResult.prepended(Entity.Declaration(name, uncheckedType(d)))

    if !partialResult.isEmpty then
      partialResult
    else if name.qualification.isDefined then
      lookupBuiltin(name)
    else
      lookupBuiltin(symbols.Name(Some(symbols.Name.builtin), name.identifier))

  /** Returns the built-in entities referred to by `name`. */
  private def lookupBuiltin(name: symbols.Name): List[Entity] =
    if name.qualification == Some(symbols.Name.builtin) then
      builtinTopLevelEntities.getOrElse(name.identifier, List())
    else
      List()

  /** A map from built-in identifier to the entities to which it may refer. */
  private val builtinTopLevelEntities: Map[String, List[Entity]] =
    val q = Some(symbols.Name.builtin)

    def entity(s: String, t: Type) = Entity.Builtin(symbols.Name(q, s), t)
    def entries(s: String, ts: Type*) = (s -> ts.map((t) => entity(s, t)).toList)

    import Type as A
    Map(
      // Built-in types
      ("Bool" -> List(Type.Bool)),
      ("Int" -> List(Type.Int)),
      ("Float" -> List(Type.Float)),
      ("String" -> List(Type.String)),
      ("Any" -> List(Type.Any)),
      ("Never" -> List(Entity.Never)),

      // IO functions
      ("exit" -> List(Entity.exit)),
      ("print" -> List(Entity.print)),

      // Universal (in)equality
      ("==" -> List(Entity.equality)),
      ("!=" -> List(Entity.inequality)),

      // Comparisons
      ("<" -> List(Entity.ilt, Entity.flt)),
      ("<=" -> List(Entity.ile, Entity.fle)),
      (">" -> List(Entity.igt, Entity.fgt)),
      (">=" -> List(Entity.ige, Entity.fge)),

      // Bool operations
      ("!" -> List(Entity.lnot)),
      ("&&" -> List(Entity.land)),
      ("||" -> List(Entity.lor)),

      // Arithmetic operations
      ("+" -> List(Entity.iadd, Entity.fadd)),
      ("-" -> List(Entity.isub, Entity.ineg, Entity.fsub, Entity.fneg)),
      ("*" -> List(Entity.imul, Entity.fmul)),
      ("/" -> List(Entity.idiv, Entity.fdiv)),
      ("%" -> List(Entity.irem)),
      ("<<" -> List(Entity.ishl)),
      (">>" -> List(Entity.ishr)),

      // Bitwise operations
      ("&" -> List(Entity.iand)),
      ("|" -> List(Entity.ior)),
      ("^" -> List(Entity.ixor)),
    )

  // --- Utilities ------------------------------------------------------------

  /** Creates an instance having the last element of `ts` as output and all elements before as
    *  inputs without labels.
    *
    *  @param ts The inputs and output of the returned value. It must be non-empty.
    */
  private def arrow(inputs: Type*)(output: Type): Type.Arrow =
    Type.Arrow(inputs.map((t) => Type.Labeled(None, t)).toList, output)

  /** Returns the entity declared by `d`. */
  private def entityDeclared(d: ast.Declaration)(using context: Typer.Context) =
    val n = nameDeclared(d)
    val t = uncheckedType(d)
    Entity.Declaration(n, t)

  /** Returns the name of the entity declared by `d`. */
  private def nameDeclared(d: ast.Declaration): symbols.Name =
    properties.declarationToNameDeclared.get(d).getOrElse({
      val q = properties.declarationToScope.get(d)
      symbols.Name(q, declaredEntityIdentifier(d))
    })

  /** Assigns the parent of `d` to innermost scope in `context`. */
  private def addToParent(d: ast.Declaration)(using context: Typer.Context): Unit =
    if !context.scopes.isEmpty then
      val parent = properties.scopeToName(context.scopes.head)
      properties.declarationToScope.put(d, parent)

  /** Assigns the name of the entity declared by `d` unless it already has one. */
  private def assignNameDeclared(d: ast.Declaration)(using context: Typer.Context): Unit =
    if properties.declarationToNameDeclared.get(d) == None then
      val q = context.scopes.headOption.flatMap(properties.scopeToName.get)
      val n = symbols.Name(q, declaredEntityIdentifier(d))
      properties.declarationToNameDeclared.put(d, n)

  /** Assigns a name to `s`, which is a scope, unless it already has one. */
  private def assignScopeName(s: ast.Tree)(using context: Typer.Context): Unit =
    if properties.scopeToName.get(s) == None then
      val q = context.scopes.headOption.flatMap(properties.scopeToName.get)
      val n = symbols.Name(q, scopeIdentifier(s))
      properties.scopeToName.put(s, n)

  /** Returns the identifier of `s`. */
  private def scopeIdentifier(s: ast.Tree): String =
    properties.scopeToName.get(s).map(_.identifier).getOrElse({
      s match
        case d: ast.Declaration => declaredEntityIdentifier(d)
        case _ => freshScopeIdentifier()
    })

  /** Returns the identifier of the entity declared by `d`. */
  private def declaredEntityIdentifier(d: ast.Declaration): String =
    d match
      case n: ast.Binding =>
        n.identifier
      case n: ast.Function =>
        n.identifier
      case n: ast.Parameter =>
        n.identifier
      case _: ast.ErrorTree =>
        unexpectedVisit(d)

  /** Returns a fresh anonymous scope identifier. */
  private def freshScopeIdentifier(): String =
    val i = nextFreshScopeIdentifier
    nextFreshScopeIdentifier += 1
    i.toString

  /** Returns a fresh type variable. */
  private def freshTypeVariable(): Type.Variable =
    val t = Type.Variable(nextFreshVariableIdentifier)
    nextFreshVariableIdentifier += 1
    t

  /** Reports the given diagnostic. */
  private def report(d: Diagnostic): Unit =
    _diagnostics = diagnostics.inserting(d)

  /** Reports the given diagnostics. */
  private def reportBatch(b: Iterable[Diagnostic]): Unit =
    _diagnostics = diagnostics.union(b)

end Typer

object Typer:

  /** The local state of type checking.
   *
   *  `declarationsIgnoredByLookup` is used to break infinite recursions caused by the type of a
   *  declaration being dependent on the name that this declaration introduces. For example, the
   *  initializer of a binding declaration cannot refer to the name introduced by that declaration.
   */
  final class Context():

    /** The lexical scopes enclosing visited nodes, innermost on the top. */
    var scopes = List[ast.Tree]()

    /** The declarations currently excluded from name lookup.
     *
     *  This property is used to break infinite recursions caused by the type of a declaration
     *  being dependent on the name that this declaration introduces. For example, the initializer
     *  of a binding declaration cannot refer to the name introduced by that declaration.
     */
    var declarationsIgnoredByLookup = List[ast.Declaration]()

    /** Unproven formulae about inferred types. */
    var obligations = ProofObligations()

    /** Returns `true` if `d` must be exclused from name lookup. */
    def isIgnoredByLookup(d: ast.Declaration): Boolean =
      declarationsIgnoredByLookup.contains(d)

    /** Returns a copy of this context with a new empty set of proof obligations. */
    def withNewProofObligations: Context =
      val clone = this
      clone.obligations = ProofObligations()
      clone

    /** Returns `action` applied on a context where `n` is the innermost scope. */
    def inScope[R](n: ast.Tree, action: Context => R): R =
      scopes = scopes.prepended(n)
      val result = action(this)
      assert(scopes.head == n)
      scopes = scopes.drop(1)
      result

    /** Returns `action` applied with `d` in the declarations to ignore during name lookup. */
    def ignoringDuringLookup[R](d: ast.Declaration, action: Context => R): R =
      declarationsIgnoredByLookup = declarationsIgnoredByLookup.prepended(d)
      val result = action(this)
      assert(declarationsIgnoredByLookup.head == d)
      declarationsIgnoredByLookup = declarationsIgnoredByLookup.drop(1)
      result

  end Context

  /** The type information computed by a solver. */
  private final class Properties:

    /** A map from tree to its unchecked type. */
    val uncheckedType = mutable.HashMap[ast.Tree, Memo[Type]]()

    /** A map from tree to its checked type. */
    val checkedType = mutable.HashMap[ast.Tree, Type]()

    /** A map from scope to its name. */
    val scopeToName = mutable.HashMap[ast.Tree, symbols.Name]()

    /** A map from declaration to the innermost scope containing it. */
    val declarationToScope = mutable.HashMap[ast.Declaration, symbols.Name]()

    /** A map from declaration to the name that it introduces. */
    val declarationToNameDeclared = mutable.HashMap[ast.Declaration, symbols.Name]()

    /** A map from tree to the entity to which it refers. */
    val treeToReferredEntity = mutable.HashMap[ast.Tree, symbols.EntityReference]()

  end Properties

end Typer
