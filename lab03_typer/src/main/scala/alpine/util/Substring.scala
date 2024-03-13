package alpine.util

import alpine.CodePoint

import scala.annotation.tailrec
import scala.collection.IntStepper
import scala.util.hashing.MurmurHash3

/** A view into a subsequence of a character string. */
final class Substring(val base: Array[CodePoint], val start: Int, val end: Int):

  require((0 <= start) && (start <= end) && (end <= base.length))

  /** `true` iff `this` is empty. `*/
  def isEmpty: Boolean =
    start == end

  /** Returns the last character of this view, if any. */
  def last: Option[CodePoint] =
    if start < end then Some(base(end - 1)) else None

  /** Returns a substring containing all but the `n` initial elements. */
  def drop(n: Int): Substring =
    Substring(base, Math.min(start + n, end), end)

  /** Returns `true` iff `this` starts with `prefix`. */
  def startsWith(prefix: String): Boolean =
    equalsString(prefix, true)

  /** Returns `true` iff `this` is equal to `other`. */
  def == (other: String): Boolean =
    equals(other)

  /** Returns `true` iff `this` is not equal to `other`. */
  def != (other: String): Boolean =
    !equals(other)

  /** Returns `true` iff `this` is equal to `other`. */
  override def equals(other: Any): Boolean =
    other match
      case rhs: Substring => equalsSubstring(rhs)
      case rhs: String => equalsString(rhs)
      case _ => false

  /** Returns `true` iff `this` is equal to `other`. */
  private def equalsSubstring(other: Substring): Boolean =
    @tailrec def loop(p: Int, q: Int): Boolean =
      if p == end then
        q == other.end
      else
        (q < other.end) && (base(p) == other.base(q)) && loop(p + 1, q + 1)

    ((base == other.base) && (start == other.start) && (end == other.end)) ||
      loop(start, other.start)

  /** Returns `true` if `this` is equal to `other` or if `this` starts with `other` and
   *  `ignoreSuffix` is true.
   */
  private def equalsString(other: String, ignoreSuffix: Boolean = false): Boolean =
    @tailrec def loop(p: Int, q: IntStepper): Boolean =
      if p == end then
        !q.hasStep
      else if !q.hasStep then
        ignoreSuffix
      else
        (base(p) == q.nextStep()) && loop(p + 1, q)
    loop(start, other.codePointStepper)

  override def hashCode(): Int =
    MurmurHash3.orderedHash(SubstringIterator(this))

  override def toString: String =
    String(base, start, end - start)

end Substring

/** An iterator producing the contents of a substring. */
final class SubstringIterator(s: Substring) extends Iterator[Int]:

  /** The underlying collection. */
  val base = s.base

  /** The first position of the substring. */
  var start = s.start

  /** The "past the end" position of the substring. */
  val end = s.end

  def hasNext: Boolean =
    start < end

  def next(): Int =
    val result = base(start)
    start += 1
    result

end SubstringIterator

/** Creates a substring with the value of `self`. */
extension (self: String) def toSubstring: Substring =
  val base = self.codePoints().toArray
  Substring(base, 0, base.length)
