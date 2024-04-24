package alpine
package typing

import alpine.ast
import alpine.symbols
import alpine.symbols.Type

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.SeqView

/** A constraint on the tree types.
 *
 *  A constraint is a predicate over one or multiple types that must be  satisfied in order for a
 *  program to be well-typed. Constraints are generated during type inference, based on the
 *  structure of the tree to which they relate.
 */
private[typing] sealed trait Constraint:

  /** The types related to this constraint, in an arbitrary order. */
  def types: Iterable[Type]

  /** The reason why the constraint was created. */
  def origin: Constraint.Origin

  /** Returns a copy of `this` in which types have been transformed by `f`. */
  def withTypeTransformed(f: Type => Type): Constraint

  /** The type variables related to this constraint, in an arbitrary order. */
  final def variables: HashSet[Type.Variable] =
    types.foldLeft(HashSet[Type.Variable]())((s, t) => s.union(t.variables))

end Constraint

private[typing] object Constraint:

  /** A constraint specifying that two types must be equal. */
  final case class Equal(lhs: Type, rhs: Type, origin: Origin) extends Constraint:

    def types: Iterable[Type] =
      IArray(lhs, rhs)

    def withTypeTransformed(f: Type => Type): Constraint =
      Equal(f(lhs), f(rhs), origin)

    override def toString: String =
      s"${lhs} == ${rhs}"

  end Equal

  /** A constraint specifying that the LHS is subtype of the RHS. */
  final case class Subtype(lhs: Type, rhs: Type, origin: Origin) extends Constraint:

    def types: Iterable[Type] =
      IArray(lhs, rhs)

    def withTypeTransformed(f: Type => Type): Constraint =
      Subtype(f(lhs), f(rhs), origin)

    override def toString: String =
      s"${lhs} <: ${rhs}"

  end Subtype

  /** A constraint specifying that F is an arrow type accepting arguments I and returning O. */
  final case class Apply(
      function: Type, inputs: List[Type.Labeled], output: Type, origin: Origin
  ) extends Constraint:

    def types: Iterable[Type] =
      List(function).prependedAll(inputs.map(_.value)).prepended(output)

    /** The argument labels of the call. */
    def labels: SeqView[Option[String]] =
      inputs.view.map(_.label)

    def withTypeTransformed(f: Type => Type): Constraint =
      Apply(f(function), inputs.map((i) => Type.Labeled(i.label, f(i.value))), f(output), origin)

    override def toString: String =
      val i = inputs.mkString(", ")
      s"(${function})($i) => ${output}"

  end Apply

  /** A constraint specifying that LHS is a type with a member M that has type RHS.
   *
   *  @param lhs The type of an entity with members.
   *  @param rhs The type of the selected member.
   *  @param member The identifier of the selected member.
   *  @param selection The expression of the member selection.
   *  @param origin The reason why the constraint is created.
   */
  final case class Member(
      lhs: Type, rhs: Type, member: String | Int, selection: ast.Expression, origin: Origin
  ) extends Constraint:

    def types: Iterable[Type] =
      IArray(lhs, rhs)

    def withTypeTransformed(f: Type => Type): Constraint =
      Member(f(lhs), f(rhs), member, selection, origin)

    override def toString: String =
      s"${lhs}.${member} == ${rhs}"

  end Member

  /** A constraint specifying that a name is a reference to one entity in an overload set.
   *
   *  @param name The expression of an overloaded entity reference.
   *  @param candidates The list of entities to which `name` can possibly refer.
   *  @param tpe The type of `name`.
   *  @param origin The reason why the constraint is created.
   */
  final case class Overload(
      name: ast.Tree, candidates: List[symbols.EntityReference], tpe: Type, origin: Origin
  ) extends Constraint:

    def types: Iterable[Type] =
      IArray(tpe)

    def withTypeTransformed(f: Type => Type): Constraint =
      Overload(name, candidates, tpe, origin)

    override def toString(): String =
      val c = candidates.map(_.entity).mkString(", ")
      s"{${c}} : ${tpe}"

  end Overload

  /** The reason why a constraint was created. */
  final case class Origin(site: SourceSpan, parent: Option[Origin] = None):

    /** The origin of a constraint derived from the constraint caused by `this`. */
    def subordinate: Origin =
      Constraint.Origin(site, Some(this))

  end Origin

end Constraint
