package alpine

import scala.annotation.tailrec
import scala.collection.immutable.Set
import scala.collection.mutable
import scala.util.control.NoStackTrace
import scala.util.Sorting

/** A set of diagnostics that can answer whether it contains an error in O(1).
 *
 *  @param elements The elements in the set.
 *  @param containsError `true` iff the set contains a diagnostic of an error.
 */
final class DiagnosticSet private (
    val elements: Set[Diagnostic],
    val containsError: Boolean
) extends Exception with NoStackTrace:

  /** `true` iff `this` is empty. */
  def isEmpty: Boolean =
    elements.isEmpty

  /** Returns a copy of `this` in which `d` has been inserted. */
  def inserting(d: Diagnostic): DiagnosticSet =
    new DiagnosticSet(elements + d, containsError || (d.level == Diagnostic.Level.Error))

  /** Returns a copy of `this` in which the elements of `b` has been inserted. */
  def union(b: DiagnosticSet): DiagnosticSet =
    union(b.elements)

  /** Returns a copy of `this` in which the elements of `b` has been inserted. */
  def union(b: Iterable[Diagnostic]): DiagnosticSet =
    @tailrec def loop(i: Iterator[Diagnostic], d: DiagnosticSet): DiagnosticSet =
      if i.hasNext then loop(i, d.inserting(i.next())) else d
    loop(b.iterator, this)

  /** Throws `this` if it contains an error. */
  def throwOnError(): Unit =
    if containsError then (throw this) else ()

  /** Writes the diagnostics in this set to the standard error. */
  def log(): Unit =
    val ds = elements.toArray
    Sorting.stableSort(ds, (a, b) => a.site < b.site)
    ds.foreach(_.log())

end DiagnosticSet

object DiagnosticSet:

  /** Creates an empty instance. */
  def apply(): DiagnosticSet =
    new DiagnosticSet(Set(), false)

  /** Creates an instance containing only `d`. */
  def apply(d: Diagnostic): DiagnosticSet =
    new DiagnosticSet(List(d).toSet, d.level == Diagnostic.Level.Error)

  /** Creates an instance containing the given diagnostics. */
  def apply(batch: Iterable[Diagnostic]): DiagnosticSet =
    val elements = mutable.HashSet[Diagnostic]()
    var containsError = false
    for d <- batch do
      if d.level == Diagnostic.Level.Error then
        containsError = true
      elements.add(d)
    new DiagnosticSet(elements.toSet, containsError)

end DiagnosticSet
