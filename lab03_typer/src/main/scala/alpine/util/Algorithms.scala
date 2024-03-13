package alpine.util

import scala.annotation.tailrec

/** Returns the start index of the partition `self` that matches `belongsInSecondPartition`.
 *
 * The collection must already be partitioned according to the predicate. That is, there should be
 * an index `i` where for every element in the range [`0`, `i`) the predicate is `false`, and for
 * every element in the range [`i`, `length`)` the predicate is `true`.
 *
 * Complexity: O(log n), where n is the length of `self`.
 */
extension[E] (self: Array[E]) def partitioningIndex(
    belongsInSecondPartition: E => Boolean
): Int =
  @tailrec def loop(n: Int, l: Int): Int =
    if n <= 0 then
      l
    else
      val half = n / 2
      val mid = l + half
      if belongsInSecondPartition(self(mid)) then
        loop(half, l)
      else
      loop(n - (half + 1), mid + 1)
  loop(self.length, 0)

/** Returns `true` iff `self` precedes `order` in a lexicographic ordering, using
 *  `areInIncreasingOrder` to compare elements.
 *
 * Complexity: O(n), where n is the length of the shortest sequence.
 */
extension[A, B >: A] (self: IterableOnce[A]) def lexicographicallyPrecedes(
    other: IterableOnce[B], areInIncreasingOrder: (A, B) => Boolean
): Boolean =
  @tailrec def loop(i: Iterator[A], j: Iterator[B]): Boolean =
    if i.hasNext then
      if !j.hasNext then false else
        val a = i.next()
        val b = j.next()
        if a == b then loop(i, j) else areInIncreasingOrder(a, b)
    else j.hasNext
  loop(self.iterator, other.iterator)
