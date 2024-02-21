package alpine

/** A value that has a source-level representation. */
trait SourceRepresentable:

  /** The site from which `this` was parsed. */
  def site: SourceSpan

end SourceRepresentable
