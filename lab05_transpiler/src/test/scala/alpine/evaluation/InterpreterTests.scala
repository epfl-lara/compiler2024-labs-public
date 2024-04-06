import alpine.driver
import alpine.SourceFile

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

class InterpreterTests extends munit.FunSuite:

  import InterpreterTests.Result

  test("Hello, World (3pts)!") {
    val input = SourceFile("test", "let main = print(\"Hello, World!\n\")")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "Hello, World!\n")
  }

  // --- Parenthesized expression ---------------------------------------------

  test("Parenthesized expressions are properly evaluated (3pts)") {
    val input = SourceFile("test", "let main = exit((1))")
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("Parenthesized expressions are properly evaluated, bis (3pts)") {
    val input = SourceFile("test", "let main = print((((((((((((42))))))))))))")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "42")
  }

  // --- Records ---------------------------------------------------------------

  test("Record fields can be accessed (3pts)") {
    val input = SourceFile("test", """
      |let record = #record(foo: 42)
      |let main = print(record.foo)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "42")
  }

  test("Multiple records can be acccessed (3pts)") {
    val input = SourceFile("test", """
      |let record1 = #record(foo: 42)
      |let record2 = #record(foo: 43)
      |let main = print(record1.foo + record2.foo)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "85")
  }

  test("Records fields are evaluated (3pts)") {
    val input = SourceFile("test", """
      |let record = #record(foo: (1 + 10))
      |let main = exit(record.foo)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 11)
  }

  // --- Conditional expressions ----------------------------------------------

  test("Conditional expressions evaluates the correct branch (3pts)") {
    {
      val input = SourceFile("test", "let main = if true then exit(42) else exit(43)")
      val Result(s, o) = interpret(input)
      assertEquals(s, 42)
    }

    {
      val input = SourceFile("test", "let main = if false then exit(42) else exit(43)")
      val Result(s, o) = interpret(input)
      assertEquals(s, 43)
    }
  }

  test("Conditional expressions evaluate the condition (3pts)") {
    {
      val input = SourceFile("test", "let main = if (1 + 1) == 2 then exit(42) else exit(43)")
      val Result(s, o) = interpret(input)
      assertEquals(s, 42)
    }

    {
      val input = SourceFile("test", "let main = if (1 + 1) == 3 then exit(42) else exit(43)")
      val Result(s, o) = interpret(input)
      assertEquals(s, 43)
    }
  }

  test("Conditional expressions have a proper result (3pts)") {
    {
      val input = SourceFile("test", """
        |let x = if true then 42 else 43
        |let main = exit(x)
        """.stripMargin)
      val Result(s, o) = interpret(input)
      assertEquals(s, 42)
    }

    {
      val input = SourceFile("test", """
        |let x = if false then 42 else 43
        |let main = exit(x)
        """.stripMargin)
      val Result(s, o) = interpret(input)
      assertEquals(s, 43)
    }
  }

  // --- Ascribed expressions -------------------------------------------------

  // test("Widening should not be an issue (3pts)") {
  //   val input = SourceFile("test", "let main = print(#record @ Any)")
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  // }

  // test("Narrowing unconditionally should not be an issue if subtype (3pts)") {
  //   val input = SourceFile("test", "let main = print(#record @! #record)")
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  // }

  // test("Narrowing unconditionally if not subtype should panic (3pts)") {
  //   val input = SourceFile("test", "let main = print((5 @ Any) @! Float)")
  //   try
  //     val r = interpret(input)
  //     assert(false, "Panic expected.")
  //   catch case _: alpine.evaluation.Panic => ()
  // }

  // test("Narrowing conditionally should return a #none if not a subtype (3pts)") {
  //   val input = SourceFile("test", "let main = print((5 @ Any) @? Float)")
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#none")
  // }

  // test("Narrowing conditionally should return a #some if a subtype (3pts)") {
  //   val input = SourceFile("test", "let main = print((1 @ Any) @? Int)")
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#some(1)")
  // }

  // --- Function calls -------------------------------------------------------

  test("Function calls with no arguments (3pts)") {
    val input = SourceFile("test", """
      |fun f() { exit(42) }
      |let main = f()
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("Function calls with a single argument (3pts)") {
    val input = SourceFile("test", """
      |fun f(_ x: Int) { exit(x) }
      |let main = f(42)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("Function arguments are evaluated (3pts)") {
    val input = SourceFile("test", """
      |fun f(_ x: Int) { exit(x) }
      |let main = f(1 + 41)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("Function calls with multiple arguments (3pts)") {
    val input = SourceFile("test", """
      |fun f(_ x: Int, _ y: Int) { exit(x - y) }
      |let main = f(20 * 2, 22 * 3)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, -26)
  }

  test("Lambda functions can be called (3pts)") {
    val input = SourceFile("test", """
      |let f = (_ x: Int) { x + 1 }
      |let main = exit(f(41))
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("Lambda functions capture the outer scope (3pts)") {
    val input = SourceFile("test", """
      |let x = 1
      |let f = (_ y: Int) { x + y }
      |let main = exit(f(41))
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  // --- Infix applications ---------------------------------------------------

  test("Infix application with 1 + 1 (3pts)") {
    val input = SourceFile("test", "let main = exit(1 + 1)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 2)
  }

  test("Infix application properly evaluates the LHS and RHS (3pts)") {
    val input = SourceFile("test", "let main = exit((1 + 1) * (2 + 2))")
    val Result(s, o) = interpret(input)
    assertEquals(s, 8)
  }

  // --- Prefix applications ---------------------------------------------------

  test("Prefix application with !true (3pts)") {
    val input = SourceFile("test", "let main = print(!true)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "false")
  }

  test("Prefix application with -42 (3pts)") {
    val input = SourceFile("test", "let main = exit(-42)")
    val Result(s, o) = interpret(input)
    assertEquals(s, -42)
  }

  // --- Let bindings ---------------------------------------------------------

  test("Let bindings are properly evaluated (3pts)") {
    val input = SourceFile("test", """
      |fun t() {
      |  let x = 42 {
      |   exit(x)
      |  }
      |}
      |let main = t()
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  // --- Match expressions ----------------------------------------------------

  test("The first pattern that matches is branched into (2pts)") {
    val input = SourceFile("test", """
      |let x = match 42 {
      |  case _ then 1
      |  case _ then 2
      |  case _ then 3
      |}
      |let main = exit(x)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("Exact match on a record is selected (2pts)") {
    val input = SourceFile("test", """
      |let x = #a(1)
      |let y = match x {
      |  case #a(1) then 1
      |  case _ then 2
      |}
      |let main = exit(y)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("Patterns are evaluated (2pts)") {
    val input = SourceFile("test", """
      |let x = match 42 {
      |  case 20 + 23 - 1 then 1
      |  case _ then 2
      |}
      |let main = exit(x)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("Bindings in pattern match are properly evaluated (2pts)") {
    val input = SourceFile("test", """
      |let x = match #a(42) {
      | case #a(let y) then y
      |}
      |let main = exit(x)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("Scrutinee is properly evaluated (2pts)") {
    val input = SourceFile("test", """
      |let x = match #a(40 + 2) {
      | case #a(42) then 1
      |}
      |let main = exit(x)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  // --- Lambda closure -------------------------------------------------------

  test("Lambda functions capture the closest outer scope (3pts)") {
    val input = SourceFile("test", """
      |let x = 10
      |fun f(_ x: Int) {
      | let p = (_ y: Int) { exit(x + y) } {
      |  p(41)
      | }
      |}
      |let main = f(1)
      """.stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  // --- Built-in functions ---------------------------------------------------

  test("exit (3pts)") {
    val input = SourceFile("test", "let main = exit(42)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 42)
  }

  test("print Boolean (3pts)") {
    val input = SourceFile("test", "let main = print(true)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "true")
  }

  test("print integer (3pts)") {
    val input = SourceFile("test", "let main = print(42)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "42")
  }

  test("print integer 2 (3pts)") {
    val input = SourceFile("test", "let main = print(42 + 1)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "43")
  }
  test("print float (3pts)") {
    val input = SourceFile("test", "let main = print(4.2)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "4.2")
  }

  test("print string (3pts)") {
    val input = SourceFile("test", "let main = print(\"42\")")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "42")
  }

  test("print record (3pts)") {
    val input = SourceFile("test", "let main = print(#a(\"x\", foo: #c(bar: 1)))")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "#a(\"x\", foo: #c(bar: 1))")
  }

  test("lnot (3pts)") {
    val input = SourceFile("test", "let main = print(!true)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "false")
  }

  test("land (3pts)") {
    val input = SourceFile("test", "let main = print(true && false)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "false")
  }

  test("inequality (3pts)") {
    val input = SourceFile("test", "let main = print(true != false)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "true")
  }

  test("equality (3pts)") {
    val input = SourceFile("test", "let main = print(true == true)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "true")
  }


  // --- Core features --------------------------------------------------------

  test("global constant (3pts)") {
    val input = SourceFile(
      "test",
      """let forty_two = 42
        |let main = print(forty_two)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "42")
  }

  test("conditional (3pts)") {
    val input = SourceFile("test", "let main = if true then exit(1) else exit(0)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("selection (3pts)") {
    val input = SourceFile("test", "let main = exit(#a(0, foo: 1).foo)")
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("let (3pts)") {
    val input = SourceFile("test", "let main = let x = 1 { exit(x) }")
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  // test("widening (3pts)") {
  //   val input = SourceFile("test", "let main = print(#a @ Any)")
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#a")
  // }

  // test("narrowing with success (3pts)") {
  //   val input = SourceFile(
  //     "test",
  //     """let x: Any = #a
  //       |let main = print(x @? #a)""".stripMargin)
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#some(#a)")
  // }

  // test("narrowing with failure (3pts)") {
  //   val input = SourceFile(
  //     "test",
  //     """let x: Any = #b
  //       |let main = print(x @? #a)""".stripMargin)
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#none")
  // }

  // test("narrowing unconditionally with success (3pts)") {
  //   val input = SourceFile(
  //     "test",
  //     """let x: Any = #a
  //       |let main = print(x @! #a)""".stripMargin)
  //   val Result(s, o) = interpret(input)
  //   assertEquals(s, 0)
  //   assertEquals(o, "#a")
  // }

  // test("narrowing unconditionally with failure (3pts)") {
  //   val input = SourceFile(
  //     "test",
  //     """let x: Any = #b
  //       |let main = print(x @! #a)""".stripMargin)
  //   try
      // val r = interpret(input)
  //     assert(false)
  //   catch case _: alpine.evaluation.Panic => ()
  // }

  test("parenthesized (3pts)") {
    val input = SourceFile("test", "let main = exit(((((1)))))")
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  // --- Extended features ----------------------------------------------------

  test("overloading (3pts)") {
    val input = SourceFile(
      "test",
      """fun f(_ x: #a) { print(#fst(x)) }
        |fun f(_ x: #b) { print(#snd(x)) }
        |let main = f(#b)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 0)
    assertEquals(o, "#snd(#b)")
  }

  test("pattern matching with exact value (3pts)") {
    val input = SourceFile(
      "test",
      """let status = match #a(1) {
        |  case (#a(2)) then 2
        |  case (#a(1)) then 1
        |  case _ then 0
        |}
        |let main = exit(status)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("pattern matching with exact record (3pts)") {
    val input = SourceFile(
      "test",
      """let status = match #a(1) {
        |  case #a(2) then 2
        |  case #a(1) then 1
        |  case _ then 0
        |}
        |let main = exit(status)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("pattern matching with wildcard (3pts)") {
    val input = SourceFile(
      "test",
      """let status = match #a(1) {
        |  case #a(_) then 1
        |  case _ then 0
        |}
        |let main = exit(status)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("pattern matching with unconstrained binding (3pts)") {
    val input = SourceFile(
      "test",
      """let status = match #a(1) {
        |  case #a(let x) then x
        |  case _ then 0
        |}
        |let main = exit(status)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("pattern matching with constrained binding (3pts)") {
    val input = SourceFile(
      "test",
      """let status = match (#a(1) @ Any) {
        |  case let x: #a(Int) then x.0
        |  case _ then 0
        |}
        |let main = exit(status)""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  test("lambda (3pts)") {
    val input = SourceFile(
      "test",
      """let f = let y = 1 { (_ x: Int) { x + y } }
        |let main = exit(f(0))""".stripMargin)
    val Result(s, o) = interpret(input)
    assertEquals(s, 1)
  }

  // --- Utilities ------------------------------------------------------------

  private def interpret(input: SourceFile): Result =
    val c = driver.Configuration(
      inputs = IArray(input),
      standardOutput = ByteArrayOutputStream())
    val s = driver.interpret(c)
    Result(s, c.standardOutput.toString)

end InterpreterTests

object InterpreterTests:

  /** The result of a interpreting a program.
   *
   *  @param status The exit status of the program.
   *  @param standardOutput The contents of the standard output.
   */
  final case class Result(status: Int, standardOutput: String)

end InterpreterTests
