package alpine.symbols

/** A reference to an entity. */
final case class EntityReference(entity: Entity, tpe: Type):

  /** Returns a copy of `this` in which types have been transformed by `f`. */
  def withTypeTransformed(f: Type => Type): EntityReference =
    EntityReference(entity.withTypeTransformed(f), f(tpe))

end EntityReference
