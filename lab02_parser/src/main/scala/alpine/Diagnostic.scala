package alpine

import scala.Console
import scala.Console.{RED, RESET, YELLOW}
import scala.util.control.NoStackTrace
import scala.util.hashing.MurmurHash3

/** A diagnostic related to a region of source code.
 *
 * @param level The severity of the diagnostic.
 * @param site The source code or source position (if empty) to which the diagnostic relates.
 */
abstract class Diagnostic(
    val level: Diagnostic.Level,
    val site: SourceSpan
) extends Exception with NoStackTrace:

  /** A short description of the diagnostic. */
  def summary: String

  /** Writes this diagnostic to the standard error. */
  final def log(): Unit =
    // The title of the diagnostic.
    Console.err.println(s"${site.gnu}: ${level.consoleDescription}: ${summary}")

    // The marked line.
    val m = site.file.lineContaining(site.start).text
    Console.err.print(m)
    if !m.last.map(isNewline).getOrElse(false) then Console.err.print("\n")

    // The column indication for that line.
    val e = Math.min(site.end, m.end)
    val w = e - site.start
    val s = e - m.start - w
    Console.err.print(" " * s)
    Console.err.println(if w <= 1 then "^" else "~" * w)

  override def equals(other: Any): Boolean =
    other match
      case rhs: Diagnostic => (summary == rhs.summary) && (site == rhs.site)
      case _ => false

  override def hashCode(): Int =
    MurmurHash3.orderedHash(List[Any](level, site, summary))

end Diagnostic

object Diagnostic:

  /** The severity of a diagnostic. */
  enum Level:

    case Warning, Error

    /** The textual description of `this` when logged to the console. */
    def consoleDescription: String =
      this match
        case Warning => s"${YELLOW}warning${RESET}"
        case Error => s"${RED}error${RESET}"

  end Level

end Diagnostic
