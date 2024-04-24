package alpine.typing

import alpine.symbols
import alpine.symbols.Type

import scala.annotation.tailrec
import scala.collection.mutable

/** A substitution mapping open type variables their assignment. */
private[typing] final class SubstitutionTable:

  /** The internal storage of the map. */
  private val assignments = mutable.HashMap[Type.Variable, Type]()

  /** Returns an optimized copy of this table. */
  def optimized: SubstitutionTable =
    val result = SubstitutionTable()
    for (v, t) <- assignments do
      result.assignments.put(v, walked(t))
    result

  /** Returns the substitution of `v`, if any. */
  def get(v: Type.Variable): Option[Type] =
    assignments.get(v).map(walked)

  /** Returns the substitution of `t` if it's a variable assigned in this table. Otherwise, returns
   *  returns `t` unchanged.
   */
  def walked(t: Type): Type =
    @tailrec def loop(w: Type): Type =
      w match
        case v: Type.Variable =>
          assignments.get(v) match
            case Some(u) => loop(u)
            case _ => v
        case v => v
    loop(t)

  /** Assigns `t` to `v`. */
  def put(v: Type.Variable, t: Type): Unit =
    @tailrec def loop(w: Type.Variable): Type.Variable =
      assignments.get(w) match
        case Some(u: Type.Variable) => loop(u)
        case Some(u) => assert(t == u); w
        case _ => w
    assignments.put(loop(v), t)

  /** Substitutes each type variable in `t` by its corresponding substitution in `this`, replacing
   *  unsassigned variables by an error.
   */
  def reify(t: Type): Type =
    reify(t, (_) => Type.Error)

  /** Substitutes each type variable in `t` by its corresponding substitution in `this`, applying
   *  `handleUnassigned` to replace variables without any assignment.
   */
  def reify(t: Type, handleUnassigned: Type.Variable => Type): Type =
    t.transformed({ (u) =>
      walked(u) match
        case v: Type.Variable => Type.StepOver(handleUnassigned(v))
        case v => if v(Type.Flags.HasVariable) then Type.StepInto(v) else Type.StepOver(v)
    })

  override def toString: String =
    "[" + assignments.map((k, v) => s"${k}: ${v}").mkString(", ") + "]"

end SubstitutionTable
