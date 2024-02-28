package alpine

import alpine.util.{Substring, partitioningIndex}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.io.Source
import scala.util.{Using, Try}

/** An Alpine source file.
 *
 * @param name The name of the file.
 * @param text The text of the file, as an array of code points.
 */
final class SourceFile(val name: String, val text: Array[CodePoint]):

  /** Creates an instance from the given string. */
  def this(name: String, text: String) =
    this(name, text.codePoints().toArray())

  /** The number of code points in this file. */
  def length: Int =
    text.length

  /** Accesses the code point at `p`. */
  def apply(p: Int): CodePoint =
    text(p)

  /** Accesses the contents of this source file in `bounds`. */
  def apply(bounds: Range): Substring =
    Substring(text, bounds.start, bounds.end)

  /** Returns a span in `this` covering [`start`, `end`). */
  def span(start: Int, end: Int): SourceSpan =
    SourceSpan(this, start, end)

  /** Returns an empty span at `p`. */
  def emptySpan(p: Int): SourceSpan =
    span(p, p)

  /** Returns the line at 1-based index `n`. */
  def line(n: Int): SourceLine =
    SourceLine(this, n)

  /** Returns the line containing the code point at `p`. */
  def lineContaining(p: Int): SourceLine =
    SourceLine(this, lineBoundaries.partitioningIndex((i) => i > p))

  /** Returns the 1-based line and column numbers of the code point at `p`. */
  def lineAndColumn(p: Int): (Int, Int) =
    val n = lineContaining(p).number
    val c = p - lineBoundaries(n - 1) + 1
    (n, c)

  /** Returns the indices of the start of each line, in order. */
  lazy val lineBoundaries: Array[Int] =
    @tailrec def loop(s: Int, b: mutable.ArrayBuilder[Int]): Array[Int] =
      (s until text.length).indexWhere((i) => isNewline(text(i))) match
        case -1 =>
          b.result
        case i =>
          val n = s + i + 1
          b += n
          loop(n, b)

    val b = new mutable.ArrayBuilder.ofInt
    b += 0
    loop(0, b)

end SourceFile

object SourceFile:

  /** Creates an instance with the contents of the file at `path`. */
  def withContentsOfFile(path: String): Try[SourceFile] =
    (Using(Source.fromFile(path)) { (f) => f.mkString })
      .map((contents) => SourceFile(path, contents))

end SourceFile
