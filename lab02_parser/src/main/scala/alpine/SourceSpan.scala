package alpine

import alpine.util.Substring

import scala.util.hashing.MurmurHash3

/** A half-open range of textual positions in a source file. */
final class SourceSpan(
    val file: SourceFile, val start: Int, val end: Int
) extends Ordered[SourceSpan]:

  import scala.math.Ordered.orderingToOrdered

  require((0 <= start) && (start <= end) && (end <= file.length))

  /** Returns `true` if this span is empty. */
  def isEmpty: Boolean =
    start == end

  /** Returns the source text contained in this range. */
  def text: Substring =
    file(start until end)

  /** Returns a description of this span per the GNU standard. */
  def gnu: String =
    val s = file.lineAndColumn(start)
    val h = s"${file.name}:${s(0)}:${s(1)}"
    if start == end then h else
      val e = file.lineAndColumn(end)
      if s(0) == e(1) then
        h + s"-${e(1)}"
      else
        h + s"-${e(0)}:${e(1)}"

  /** Returns a span covering `this` and the positions until `extendedEnd`. */
  def extendedTo(extendedEnd: Int): SourceSpan =
    SourceSpan(file, start, extendedEnd)

  /** Returns a span covering `this` and `other`. */
  def extendedToCover(other: SourceSpan): SourceSpan =
    require(file == other.file)
    SourceSpan(file, start, other.end)

  /** Returns the result of comparing `this` with `other`. */
  def compare(other: SourceSpan): Int =
    (file.name, start, end).compare((other.file.name, other.start, other.end))

  /** Returns `true` iff `this` is equal to `other`. */
  override def equals(other: Any): Boolean =
    other match
      case rhs: SourceSpan => (file == rhs.file) && (start == rhs.start) && (end == rhs.end)
      case _ => false

  override def hashCode(): Int =
    MurmurHash3.orderedHash(List[Any](file, start, end))

  /** A textual representation of this span suitable for debugging. */
  override def toString: String =
    gnu

end SourceSpan

object SourceSpanOrdering extends Ordering[SourceSpan]:

  def compare(a: SourceSpan, b: SourceSpan) = a.compare(b)

end SourceSpanOrdering