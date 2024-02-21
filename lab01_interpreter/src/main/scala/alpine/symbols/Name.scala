package alpine.symbols

/** The qualified identifier of a named symbol. */
final case class Name(qualification: Option[Name], identifier: String):

  override def toString: String =
    qualification match
      case Some(q) => s"${q}.${identifier}"
      case _ => identifier

end Name

object Name:

  val builtin = Name(None, "Builtin")

end Name
