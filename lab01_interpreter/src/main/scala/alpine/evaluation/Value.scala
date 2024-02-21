package alpine.evaluation

import alpine.ast
import alpine.symbols.{Type, Name}

/** An Alpine value at run-time. */
sealed trait Value:

  /** The type of the value at run-time. */
  def dynamicType: Type

end Value

object Value:

  /** The `#unit` value. */
  val unit = Record("#unit", List(), Type.Unit)

  /** A built-in function. */
  final case class BuiltinFunction(identifier: String, dynamicType: Type.Arrow) extends Value:

    override def toString: String =
      s"Builtin.${identifier}"

  end BuiltinFunction

  /** A built-in value. */
  final case class Builtin[T](value: T, dynamicType: Type.Builtin) extends Value:

    override def toString: String =
      value.toString

  end Builtin

  /** A record value. */
  final case class Record(
      identifier: String, fields: List[Value], dynamicType: Type.Record
  ) extends Value:

    override def toString: String =
      if fields.isEmpty then identifier else
        val a = fields.zip(dynamicType.fields)
          .map((v, t) => t.label.map((l) => s"${l}: ${v}").getOrElse(v.toString))
          .mkString(", ")
        s"${identifier}(${a})"

  end Record

  /** A function. */
  final case class Function(body: ast.Function, dynamicType: Type) extends Value

  /** A lambda (i.e., an anonymous function and its captures). */
  final case class Lambda(
      body: ast.Expression, inputs: List[ast.Parameter], captures: Map[Name, Value],
      dynamicType: Type
  ) extends Value

  /** An unevaluated value. */
  final case class Unevaluated(declaration: ast.Declaration, dynamicType: Type) extends Value

  /** A poison value. */
  case object Poison extends Value:

    def dynamicType: Type =
      Type.Never

  end Poison

  /** Returns the `#none` case of an option. */
  def none: Record =
    Record("#none", List(), Type.none)

  /** Returns the `#some` case of an option wrapping `w`. */
  def some(w: Value): Record =
    Record("#some", List(w), Type.some(w.dynamicType))

end Value
