package alpine.typing

import alpine.ast
import alpine.symbols
import alpine.symbols.Type

import scala.collection.mutable

/** A set of formulae to be proven for type checking a declaration, expression, or pattern. */
private[typing] final class ProofObligations:

  /** A map from tree to its type. */
  private val _inferredType = mutable.HashMap[ast.Tree, Type]()

  /** A map from tree to its binding. */
  private val _inferredBinding = mutable.HashMap[ast.Tree, symbols.EntityReference]()

  /** A map from type variable to its upper bound. */
  private val upperBound = mutable.HashMap[Type.Variable, Type]()

  /** A set of constraints. */
  private val _constraints = mutable.LinkedHashSet[Constraint]()

  /** `true` iff at least one formula in this set is known to be unprovable. */
  private var _isUnsatisfiable = false

  /** A map from tree to its inferred type. */
  def inferredType: Map[ast.Tree, Type] = _inferredType.toMap

  /** A map from tree to its binding. */
  def inferredBinding: Map[ast.Tree, symbols.EntityReference] = _inferredBinding.toMap

  /** A set of constraints. */
  def constraints: scala.collection.View[Constraint] = _constraints.view

  /** `true` iff at least one formula in this set is known to be unprovable. */
  def isUnsatisfiable: Boolean = _isUnsatisfiable

  /** `true` iff `this` is empty. */
  def isEmpty: Boolean =
   _inferredType.isEmpty && upperBound.isEmpty && _constraints.isEmpty

  /** Clears the contents of this set. */
  def clear(): Unit =
    _inferredType.clear()
    upperBound.clear()
    _constraints.clear()
    _isUnsatisfiable = false

  /** Inserts `c` in this set and returns `true` iff it wasn't already contained. */
  def add(c: Constraint): Boolean =
    if !_constraints.add(c) then false else
      updateKnownBounds(c)
      true

  /** Binds `n` to the entity referred to by `r`. */
  def bind(n: ast.Tree, r: symbols.EntityReference): Unit =
    _inferredBinding.put(n, r)

  /** Constrains `n` to have type `t`, returning `t`. */
  def constrain(n: ast.Tree, t: Type): Type =
    if t(Type.Flags.HasError) then
      _isUnsatisfiable = true

    _inferredType.get(n) match
      case Some(u) =>
        if t != u then add(Constraint.Equal(t, u, Constraint.Origin(n.site)))
        u
      case _ =>
        _inferredType.put(n, t)
        t

  /** Updates the known bounds of the type variables in this set given `insertedConstraint`. */
  private def updateKnownBounds(insertedConstraint: Constraint): Unit =
    import Constraint.{Equal, Subtype}
    import Type.Variable

    insertedConstraint match
      case Equal(lhs: Variable, rhs, _) =>
        upperBound.put(lhs, rhs)
      case Equal(lhs, rhs: Variable, _) =>
        upperBound.put(rhs, lhs)
      case Subtype(lhs: Variable, rhs, _) =>
        upperBound.put(lhs, rhs)
      case _ => ()

end ProofObligations