package alpine.symbols

import alpine.ast

/** An entity that can be referred to by an identifier. */
trait Entity:

  /** The name of the entity. */
  def name: Name

  /** The type of the entity. */
  def tpe: Type

end Entity

object Entity:

  /** A built-in entity. */
  final case class Builtin(name: Name, tpe: Type) extends Entity

  /** A declaration defined in source. */
  final case class Declaration(name: Name, tpe: Type) extends Entity

  /** A field of a record type. */
  final case class Field(whole: Type.Record, index: Int) extends Entity:

    def name: Name =
      Name(Some(Name(None, whole.toString)), index.toString)

    def tpe: Type =
      whole.fields(index).value

  end Field

  /** The built-in module. */
  val builtinModule = Builtin(Name.builtin, Type.BuiltinModule)

  /** The built-in `Never` type. */
  val Never = builtin("Never", Type.Meta(Type.Never))

  /** The built-in `exit` function. */
  val exit = builtin("exit", Type.Arrow(List(Type.Labeled(None, Type.Int)), Type.Never))

  /** The built-in `print` function. */
  val print = builtin("print", Type.Arrow(List(Type.Labeled(None, Type.Any)), Type.Unit))

  /** Universal equality. */
  val equality = builtin("equality", Type.Arrow.comparison(Type.Any))

  /** Universal inequality. */
  val inequality = builtin("inequality", Type.Arrow.comparison(Type.Any))

  /** Logical negation. */
  val lnot = builtin("lnot", Type.Arrow.unary(Type.Bool))

  /** Logical conjunction. */
  val land = builtin("land", Type.Arrow.binary(Type.Bool))

  /** Logical disjunction. */
  val lor = builtin("lor", Type.Arrow.binary(Type.Bool))

  /** Integer negation. */
  val ineg = builtin("ineg", Type.Arrow.unary(Type.Int))

  /** Integer addition. */
  val iadd = builtin("iadd", Type.Arrow.binary(Type.Int))

  /** Integer subtraction. */
  val isub = builtin("isub", Type.Arrow.binary(Type.Int))

  /** Integer multiplication. */
  val imul = builtin("imul", Type.Arrow.binary(Type.Int))

  /** Integer division. */
  val idiv = builtin("idiv", Type.Arrow.binary(Type.Int))

  /** Integer remainder. */
  val irem = builtin("irem", Type.Arrow.binary(Type.Int))

  /** Integer left shift. */
  val ishl = builtin("ishl", Type.Arrow.binary(Type.Int))

  /** Integer right shift. */
  val ishr = builtin("ishr", Type.Arrow.binary(Type.Int))

  /** Integer less than comparison. */
  val ilt = builtin("ilt", Type.Arrow.binary(Type.Int))

  /** Integer less than or equal to comparison. */
  val ile = builtin("ile", Type.Arrow.binary(Type.Int))

  /** Integer greater than comparison. */
  val igt = builtin("igt", Type.Arrow.binary(Type.Int))

  /** Integer greater than or equal to comparison. */
  val ige = builtin("ige", Type.Arrow.binary(Type.Int))

  /** Integer bitwise inversion. */
  val iinv = builtin("iinv", Type.Arrow.unary(Type.Int))

  /** Integer bitwise AND. */
  val iand = builtin("iand", Type.Arrow.binary(Type.Int))

  /** Integer bitwise OR. */
  val ior = builtin("ior", Type.Arrow.binary(Type.Int))

  /** Integer bitwise XOR. */
  val ixor = builtin("ixor", Type.Arrow.binary(Type.Int))

  /** Floating-point negation. */
  val fneg = builtin("fneg", Type.Arrow.unary(Type.Float))

  /** Floating-point addition. */
  val fadd = builtin("fadd", Type.Arrow.binary(Type.Float))

  /** Floating-point subtraction. */
  val fsub = builtin("fsub", Type.Arrow.binary(Type.Float))

  /** Floating-point multiplication. */
  val fmul = builtin("fmul", Type.Arrow.binary(Type.Float))

  /** Floating-point division. */
  val fdiv = builtin("fdiv", Type.Arrow.binary(Type.Float))

  /** Floating-point less than comparison. */
  val flt = builtin("flt", Type.Arrow.binary(Type.Float))

  /** Floating-point less than or equal to comparison. */
  val fle = builtin("fle", Type.Arrow.binary(Type.Float))

  /** Floating-point greater than comparison. */
  val fgt = builtin("fgt", Type.Arrow.binary(Type.Float))

  /** Floating-point greater than or equal to comparison. */
  val fge = builtin("fge", Type.Arrow.binary(Type.Float))

  /** Returns a built-in entity having identifier `s` and type `t`. */
  private def builtin(s: String, t: Type): Entity =
    Entity.Builtin(Name(Some(Name.builtin), s), t)

end Entity
