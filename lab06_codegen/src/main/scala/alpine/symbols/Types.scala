package alpine.symbols

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.SeqView

trait Type:

  /** Information about this instance. */
  def flags: Type.Flags =
    Type.Flags()

  /** Returns `true` iff `this` contains the flags in `properties`. */
  final def apply(properties: Type.Flags): Boolean =
    flags(properties)

  /** Returns `true` iff `this` is an arrow. */
  final def isArrow: Boolean =
    this match
      case _: Type.Arrow => true
      case _ => false

  /** Returns `true` iff `this` is built.in. */
  final def isBuiltin: Boolean =
    this match
      case _: Type.Builtin => true
      case _ => false

  /** Returns `true` iff `this` is a variable. */
  final def isVariable: Boolean =
    this match
      case _: Type.Variable => true
      case _ => false

  /** Returns this type transformed with `transformer`.
   *
   *  This method visits the structure of the type and calls `transformer` on each type composing
   *  that structure. The result of the call is substituted for the visited type. If `transformer`
   *  returns `StepInto(t)`, the visited type is replaced by the result of `transformed` applied on
   *  `t` and `transformer`. If it returns `StepOver(t)`, the visited type is replaced by `t` and
   *  the visit moves on to the next type in the structure.
   */
  final def transformed(transformer: Type => Type.TransformAction): Type =
    transformer(this) match
      case Type.StepInto(t) => t.withPartsTransformed(transformer)
      case Type.StepOver(t) => t

  /** Applies [[this.transform]] on the constituent types of `this`. */
  def withPartsTransformed(transformer: Type => Type.TransformAction): Type =
    this

  /** Returns `true` iff `this` is subtype of `other`. */
  def isSubtypeOf(other: Type): Boolean =
    (other == Type.Any) || (this == other)

  /** Returns `true` if `this` matches `other`, comparing different parts with `compare`.
   *
   *  `this` and `other` are visited "side-by-side", calling `compare` iff:
   *    - `this` isn't equal to `other` (by `==`); and
   *    - both `this` and `other` are non-structural type terms _or_ either `this` or `other` is a
   *      type variable.
   *
   * @param other The type that is matched with `this`.
   * @param compare A closure that returns `true` iff `this` and `other` should be considered
   *        equivalent for the purpose of the call.
   */
  def matches(other: Type, compare: (Type, Type) => Boolean): Boolean =
    (this == other) || compare(this, other)

  /** Returns the result of `f(this)` if `this` isn't `Type.Error`. Otherwise, retuns `this`. */
  final def map(f: Type => Type): Type =
    if this == Type.Error then this else f(this)

  /** Returns the free type variables that occur in `this`. */
  final def variables: HashSet[Type.Variable] =
    var partialResult = HashSet[Type.Variable]()
    transformed({(t) =>
      t match
          case u: Type.Variable =>
            partialResult = partialResult.incl(u)
            Type.StepOver(u)
          case _ =>
            if t(Type.Flags.HasVariable) then Type.StepInto(t) else Type.StepOver(t)
    })
    partialResult

end Type

object Type:

  /** A built-in type. */
  sealed trait Builtin extends Type with Entity:

    final def name: Name =
      Name(Some(Name.builtin), getClass.getSimpleName)

    final def tpe: Type =
      Meta(this)

    def withTypeTransformed(f: Type => Type): Entity =
      require(f(this) == this)
      this

  end Builtin

  /** The result of type error. */
  case object Error extends Type:

    override def flags: Type.Flags =
      Type.Flags.HasError

  end Error

  /** The built-in module. */
  case object BuiltinModule extends Builtin

  /** The built-in type `Bool`. */
  case object Bool extends Builtin

  /** The built-in type `Int`. */
  case object Int extends Builtin

  /** The built-in type `Float`. */
  case object Float extends Builtin

  /** The built-in type `String`. */
  case object String extends Builtin

  /** The `Any` type. */
  case object Any extends Builtin

  /** The `Never` type, which is uninhabited. */
  val Never = Sum.empty

  /** The `#unit` type. */
  val Unit = Record("#unit", List())

  /** An open type variable. */
  final case class Variable(id: Int) extends Type:

    override def flags: Type.Flags =
      Type.Flags.HasVariable

    override def toString: String =
      "$" + id.toString

  end Variable

  /** The type of a type. */
  final case class Meta(instance: Type) extends Type:

    override def flags: Type.Flags =
      instance.flags

    override def withPartsTransformed(transformer: Type => Type.TransformAction): Type =
      Meta(instance.transformed(transformer))

    override def matches(other: Type, compare: (Type, Type) => Boolean): Boolean =
      other match
        case Meta(rhs) => instance.matches(rhs, compare)
        case v: Variable => compare(this, v)
        case _ => false

    override def toString: String =
      s"Meta(${instance})"

  end Meta

  /** A type defined in source. */
  final case class Definition(identifier: String) extends Type

  /** The type of a record. */
  final case class Record(val identifier: String, val fields: List[Labeled]) extends Type:

    /** The number of fields in this record. */
    def length: Int =
      fields.length

    /** The field labels of this record. */
    def labels: SeqView[Option[String]] =
      fields.view.map(_.label)

    /** Returns `true` iff the identifier and field labels of `this` match those of `other`. */
    def structurallyMatches(other: Record): Boolean =
      (this.identifier == other.identifier) &&
        this.fields.map(_.label).sameElements(other.fields.map(_.label))

    /** Returns `true` iff `this` is ordered before `other` when stored in a sum. */
    def structurallyPrecedes(other: Record): Boolean =
      import alpine.util.lexicographicallyPrecedes
      this.identifier < other.identifier ||
        this.length < other.length ||
        this.labels.lexicographicallyPrecedes(
          other.labels,
          (a, b) => a.map((x) => b.map((y) => x < y).getOrElse(false)).getOrElse(b.isDefined))

    override val flags: Type.Flags =
      fields.foldLeft(Type.Flags())((a, b) => a.union(b.value.flags))

    override def withPartsTransformed(transformer: Type => Type.TransformAction): Type =
      Record(identifier, fields.map((f) => Labeled(f.label, f.value.transformed(transformer))))

    override def isSubtypeOf(other: Type): Boolean =
      other match
        case s: Type.Sum => s.members.exists((m) => this.isSubtypeOf(m))
        case _ => super.isSubtypeOf(other)

    override def matches(other: Type, compare: (Type, Type) => Boolean): Boolean =
      other match
        case rhs: Record =>
          (identifier == rhs.identifier) && Type.matchesLabeled(fields, rhs.fields, compare)
        case rhs: Variable => compare(this, rhs)
        case _ => false

    override def toString: String =
      if fields.isEmpty then identifier else
        val a = fields.mkString(", ")
        s"${identifier}(${a})"

  end Record

  object Record:

    /** Returns a copy of `other` iff it is an instance of `Record`. */
    def from(other: Type): Option[Record] =
      other match
        case a: Record => Some(a)
        case _ => None

  end Record

  /** The type of a function or lambda. */
  final case class Arrow(val inputs: List[Labeled], val output: Type) extends Type:

    /** The argument labels of arrow. */
    def labels: SeqView[Option[String]] =
      inputs.view.map(_.label)

    override val flags: Type.Flags =
      inputs.foldLeft(output.flags)((a, b) => a.union(b.value.flags))

    override def withPartsTransformed(transformer: Type => Type.TransformAction): Type =
      Arrow(
        inputs.map((i) => Labeled(i.label, i.value.transformed(transformer))),
        output.transformed(transformer))

    override def matches(other: Type, compare: (Type, Type) => Boolean): Boolean =
      other match
        case rhs: Arrow =>
          Type.matchesLabeled(inputs, rhs.inputs, compare) && output.matches(rhs.output, compare)
        case rhs: Variable => compare(this, rhs)
        case _ => false

    override def toString: String =
      val i = inputs.mkString(", ")
      s"(${i}) -> ${output}"

  end Arrow

  object Arrow:

    /** Returns a copy of `other` iff it is an instance of `Arrow`. */
    def from(other: Type): Option[Arrow] =
      other match
        case a: Arrow => Some(a)
        case _ => None

    /** Returns the type of a unary operation on `t`. */
    def unary(t: Type): Arrow =
      Arrow(List(Labeled(None, t)), t)

    /** Returns the type of a binary operation on `t`. */
    def binary(t: Type): Arrow =
      Arrow(List(Labeled(None, t), Labeled(None, t)), t)

    /** Returns the type of a comparison operation on `t`. */
    def comparison(t: Type): Arrow =
      Arrow(List(Labeled(None, t), Labeled(None, t)), Bool)

  end Arrow

  /** A sum type. */
  final class Sum private (val members: List[Type.Record]) extends Type:

    /** Returns a copy of `this` in which `d` has been inserted iff `m` doesn't structually matches
     *  any of the elments in `this`.
     */
    def inserting(m: Type.Record): Option[Sum] =
      type M = List[Type.Record]
      @tailrec def newMembers(l: M, r: M): Option[M] =
        r match
          case h :: t =>
            if m.structurallyMatches(h) then
              None
            else if m.structurallyPrecedes(h) then
              Some(r.prepended(m).prependedAll(l.reverseIterator))
            else if m == h then
              Some(r.prependedAll(l.reverseIterator))
            else
              newMembers(l.prepended(h), t)
          case Nil =>
            Some(l.prepended(m).reverseIterator.toList)
      newMembers(List(), members).map((ms) => new Sum(ms))

    /** Returns a sum containing the elements of both `this` and `other` iff none of them
     *  structurally match each other.
     */
    def union(other: Sum): Option[Sum] =
      @tailrec def loop(clone: Sum, newMembers: List[Type.Record]): Option[Sum] =
        newMembers match
          case h :: t =>
            clone.inserting(h) match
              case Some(s) => loop(s, t)
              case _ => None
          case Nil => None
      loop(this, other.members)

    /** Returns a sum containing the elements that are common to both `this` and `other`. */
    def intersect(other: Sum): Sum =
      def newMembers(lhs: List[Type.Record], rhs: List[Type.Record]): List[Type.Record] =
        lhs match
          case l :: ls => rhs match
            case r :: rs =>
              val t = newMembers(ls, rs)
              if l == r then t.prepended(l) else t
            case Nil => List()
          case Nil => List()
      new Sum(newMembers(this.members, other.members))

    override val flags: Type.Flags =
      members.foldLeft(Type.Flags())((a, b) => a.union(b.flags))

    override def withPartsTransformed(transformer: Type => Type.TransformAction): Type =
      Sum(members.map((m) => Record.from(m.transformed(transformer)).get))

    override def isSubtypeOf(other: Type): Boolean =
      other match
        case s: Type.Sum => isSubtypeOfSum(s)
        case _ => members.isEmpty || super.isSubtypeOf(other)

    /** Returns `true` iff `this` is subtype of `other`. */
    def isSubtypeOfSum(other: Type.Sum): Boolean =
      @tailrec def loop(ls: List[Type.Record], rs: List[Type.Record]): Boolean =
        ls match
          case Nil => true
          case l :: lt => rs match
            case Nil => false
            case r :: rt =>
              if l.structurallyMatches(r) then
                l.isSubtypeOf(r) && loop(lt, rt)
              else if l.structurallyPrecedes(r) then
                loop(ls, rt)
              else
                false
      loop(members, other.members)

    override def matches(other: Type, compare: (Type, Type) => Boolean): Boolean =
      other match
        case rhs: Sum => Type.matches(members, rhs.members, compare)
        case rhs: Variable => compare(this, rhs)
        case _ => false

    override def toString: String =
      if members.isEmpty then "Never" else members.mkString(" | ")

  end Sum

  object Sum:

    /** An empty sum. */
    val empty = new Sum(List())

    /** Returns a sum containing `a` and `b` iff they don't structurally match. */
    def fromPair(a: Type.Record, b: Type.Record): Option[Sum] =
      (new Sum(List(a))).inserting(b)

    def from(tpe: Type): Option[Sum] =
      tpe match
        case s: Sum => Some(s)
        case _ => None

  end Sum

  /** A type symbol with an optional label. */
  final case class Labeled(label: Option[String], value: Type):

    override def toString: String =
      label match
        case Some(s) => s"${s}: ${value}"
        case _ => value.toString

  end Labeled

  /** Information about a type.
   *
   * @param rawValue The raw value this instance, which is used as a bitset.
   */
  final case class Flags private (val rawValue: Int):

    /** Returns a set containing the flags in `this` together with those in `other`. */
    def union(other: Flags): Flags =
      new Flags(this.rawValue | other.rawValue)

    /** Returns `true` iff `this` contains the same flags as `other`. */
    def apply(other: Flags): Boolean =
      (rawValue & other.rawValue) == other.rawValue

    override def toString: String =
      s"Flags(${rawValue.toString})"

  end Flags

  object Flags:

    /** Creates an empty instance. */
    def apply(): Flags =
      new Flags(0)

    /** A type with an error. */
    val HasError = new Flags(1)

    /** A type that contains an open type variable. */
    val HasVariable = new Flags(2)

  end Flags

  /** The result of a call to a closure passed to [[Type.transform]]. */
  sealed trait TransformAction

  /** Instructs a type transformer to step into `t`. */
  final case class StepInto(t: Type) extends TransformAction

  /** Instructs a type transformer to step over the current type and produce `t`. */
  final case class StepOver(t: Type) extends TransformAction

  /** Returns `true` if `this` matches `other`, comparing different parts with `compare`. */
  private def matches(
      lhs: Iterable[Type], rhs: Iterable[Type],
      compare: (Type, Type) => Boolean
  ): Boolean =
    lhs.corresponds(rhs)((a, b) => a.matches(b, compare))

  import scala.collection.IterableOnceOps

  /** Returns `true` if `this` matches `other`, comparing different parts with `compare`. */
  private def matchesLabeled(
      lhs: Iterable[Labeled], rhs: Iterable[Labeled],
      compare: (Type, Type) => Boolean
  ): Boolean =
    lhs.corresponds(rhs)((a, b) => (a.label == b.label) && a.value.matches(b.value, compare))

  /** Returns the type of an option wrapping instances of `t`. */
  def option(t: Type): Sum =
    Sum.empty
      .inserting(none).get
      .inserting(some(t)).get

  /** Returns the `#none` case of an option. */
  def none: Record =
    Record("#none", List())

  /** Returns the `#some` case of an option wrapping instances of `t`. */
  def some(t: Type): Record =
    Record("#some", List(Labeled(None, t)))

end Type
