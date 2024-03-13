package alpine

import alpine.util.Substring

import scala.util.hashing.MurmurHash3

/** A line of a source file.
 *
 * @param file The source file containing the line.
 * @param number The 1-based index of the line in `file`.
 */
final class SourceLine(val file: SourceFile, val number: Int):

  /** Returns the bounds of this line, including any trailing newline. */
  def bounds: SourceSpan =
    val s = file.lineBoundaries(number - 1)
    if number < file.lineBoundaries.length then
      SourceSpan(file, s, file.lineBoundaries(number))
    else
      SourceSpan(file, s, file.length)

  /** Returns the source text contained in this line. */
  def text: Substring =
    bounds.text

  override def equals(other: Any): Boolean =
    other match
      case rhs: SourceLine => (file == rhs.file) && (number == rhs.number)
      case _ => false

  override def hashCode(): Int =
    MurmurHash3.orderedHash(List[Any](file, number))

  override def toString: String =
    text.toString

end SourceLine
