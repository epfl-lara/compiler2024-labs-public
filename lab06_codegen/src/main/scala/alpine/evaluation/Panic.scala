package alpine
package evaluation

/** An error that caused the inerpreter to panic. */
final class Panic(val message: String) extends Exception:

  override def getMessage(): String =
    message

end Panic
