import alpine.util.lexicographicallyPrecedes
import alpine.util.partitioningIndex

import scala.math.Ordering

class AlgorithmsTests extends munit.FunSuite:

  test("partitioningIndex[Array] (0pt)") {
    for
      i <- 0 until 5
      j <- i until 9
      k <- i until (j + 1)
    do
      val a = (i until j).toArray
      val p = i + a.partitioningIndex((x) => x >= k)
      assert(p >= i)
      assert(p <= j)
      assertEquals(p, k)
  }

  test("lexicographicallyPrecedes[Array] empty (0pt)") {
    val empty = Array[Int]()
    val nonEmpty = Array[Int](1)

    assert(!empty.lexicographicallyPrecedes(empty, Ordering.Int.lt))
    assert(!nonEmpty.lexicographicallyPrecedes(empty, Ordering.Int.lt))
    assert(empty.lexicographicallyPrecedes(nonEmpty, Ordering.Int.lt))
  }

  test("lexicographicallyPrecedes[Array] non-empty (0pt)") {
    val a = Array[Int](1)
    val aa = Array[Int](1, 1)
    val ab = Array[Int](1, 2)

    assert(!a.lexicographicallyPrecedes(a, Ordering.Int.lt))
    assert(!ab.lexicographicallyPrecedes(aa, Ordering.Int.lt))

    assert(a.lexicographicallyPrecedes(ab, Ordering.Int.lt))
    assert(aa.lexicographicallyPrecedes(ab, Ordering.Int.lt))
  }

end AlgorithmsTests
