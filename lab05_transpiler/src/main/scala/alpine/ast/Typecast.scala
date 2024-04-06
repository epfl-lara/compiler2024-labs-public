package alpine.ast

/** The kind of operation performed by an ascription expression. */
enum Typecast:

  /** The static type of the value is widened to a supertype. */
  case Widen

  /** The static type of the value is narrowed to a subtype, returning an option. */
  case Narrow

  /** The static type of the value is narrowed to a subtype unconditionally. */
  case NarrowUnconditionally

end Typecast
