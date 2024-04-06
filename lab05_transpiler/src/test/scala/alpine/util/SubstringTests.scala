import alpine.util.{Substring, toSubstring}

class SubstringTests extends munit.FunSuite:

  private val base = "abcabc".codePoints().toArray

  test("isEmpty (0pt)") {
    assert(Substring(base, 1, 1).isEmpty)
    assert(!Substring(base, 1, 3).isEmpty)
  }

  test("last (0pt)") {
    assertEquals(Substring(base, 1, 1).last, None)
    assertEquals(Substring(base, 1, 3).last, Some('c'.toInt))
  }

  test("drop (0pt)") {
    val s = Substring(base, 1, 5)
    assertEquals(s.drop(1).toString, "cab")
    assertEquals(s.drop(2).toString, "ab")
    assertEquals(s.drop(10).toString, "")
  }

  test("startsWith (0pt)") {
    val s = Substring(base, 0, 5)
    assert(s.startsWith("abc"))
    assert(!s.startsWith("ab_"))
    assert(!s.startsWith("abc_"))
  }

  test("equals[Substring] (0pt)") {
    assert(Substring(base, 1, 1) == Substring(base, 2, 2))
    assert(Substring(base, 1, 3) == Substring(base, 4, 6))
    assert(Substring(base, 1, 3) != Substring(base, 2, 2))
    assert(Substring(base, 0, 2) != Substring(base, 3, 6))
  }

  test("equals[String] (0pt)") {
    assert(Substring(base, 1, 1) == "")
    assert(Substring(base, 1, 3) == "bc")
    assert(Substring(base, 1, 3) != "")
    assert(Substring(base, 1, 3) != "bd")
  }

  test("hashCode (0pt)") {
    def s = Substring(base, 1, 3)
    def t = Substring(base, 4, 6)
    assertEquals(s.hashCode, s.hashCode)
    assertEquals(s.hashCode, t.hashCode)
    assertNotEquals(s.hashCode, Substring(base, 3, 3).hashCode)
  }

  test("toString (0pt)") {
    assertEquals(Substring(base, 1, 1).toString, "")
    assertEquals(Substring(base, 1, 3).toString, "bc")
  }

  test("toSubstring (0pt)") {
    assertEquals("abc".toSubstring.toString, "abc")
  }

end SubstringTests
