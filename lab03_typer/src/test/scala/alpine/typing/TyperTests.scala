package alpine.typing

import alpine._
import alpine.ast
import alpine.typing.Typer
import alpine.SourceSpan
import alpine.symbols.Type
import alpine.typing.Constraint


class TyperTests extends munit.FunSuite:
  val dummySite = SourceSpan(SourceFile("dummy", Array[CodePoint]()), 0, 0)
  val dummyProgram = alpine.Program(IArray[ast.Declaration]())

  test("`visitRecord` should return the correct type of a #record(1) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assertEquals(result, Type.Record("record", List(Type.Labeled(None, Type.Int))))
  }

  test("`visitRecord` should return the correct type of a #record(1, 2) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("2", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assertEquals(result, Type.Record("record", List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int))))
  }

  test("`visitRecord` should return the correct type of a #record(1, 2, 3) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("2", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("3", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assertEquals(result, Type.Record("record", List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int))))
  }

  test("`visitRecord` should return the correct type of a #record(\"a\", true) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.StringLiteral("a", dummySite), dummySite), ast.Labeled(None, ast.BooleanLiteral("true", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assertEquals(result, Type.Record("record", List(Type.Labeled(None, Type.String), Type.Labeled(None, Type.Bool))))
  }

  test("`visitRecord` should constrain on the type of a #record(1) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    println()
    assert(
      ctx.obligations.inferredType(record) == result
    )
  }

  test("`visitRecord` should constrain on the type of a #record(1, 2) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("2", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assert(
      ctx.obligations.inferredType(record) == result
    )
  }

  test("`visitRecord` should constrain on the type of a #record(1, 2, 3) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("2", dummySite), dummySite), ast.Labeled(None, ast.IntegerLiteral("3", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assert(
      ctx.obligations.inferredType(record) == result
    )
  }

  test("`visitRecord` should constrain on the type of a #record(\"a\", true) (3pts)") {
    val record = ast.Record("record", List(ast.Labeled(None, ast.StringLiteral("a", dummySite), dummySite), ast.Labeled(None, ast.BooleanLiteral("true", dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecord(record)(using ctx)
    assert(
      ctx.obligations.inferredType(record) == result
    )
  }

  test("`visitApplication` adds an apply constraint with a known function (3pts)") {
    val f = ast.Identifier("exit", dummySite)
    val application = ast.Application(f, List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite)), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitApplication(application)(using ctx)
    assert(
      ctx.obligations.constraints.exists(p =>
        p match
          case Constraint.Apply(
              Type.Arrow(List(Type.Labeled(None, Type.Int)), Type.Never),
              List(Type.Labeled(None, Type.Int)),
              Type.Never,
              _
            ) => true
          case _ => false
      )
    )
  }

  test("`visitApplication` sets the type of the application to the return type of the function (3pts)") {
    val f = ast.Identifier("exit", dummySite)
    val application = ast.Application(f, List(ast.Labeled(None, ast.IntegerLiteral("1", dummySite), dummySite)), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitApplication(application)(using ctx)
    assertEquals(ctx.obligations.inferredType(application), Type.Never)
  }

  test("`visitPrefixApplication` adds an apply constraint with a known function (3pts)") {
    val f = ast.Identifier("!", dummySite)
    val application = ast.PrefixApplication(f, ast.BooleanLiteral("false", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitPrefixApplication(application)(using ctx)
    assert(
      ctx.obligations.constraints.exists(p =>
        p match
          case Constraint.Apply(
              Type.Arrow(List(Type.Labeled(None, Type.Bool)), Type.Bool),
              List(Type.Labeled(None, Type.Bool)),
              Type.Bool,
              _
            ) => true
          case _ => false
      )
    )
  }

  test("`visitPrefixApplication` adds a constraint on the output type of the application (3pts)") {
    val f = ast.Identifier("!", dummySite)
    val application = ast.PrefixApplication(f, ast.BooleanLiteral("false", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitPrefixApplication(application)(using ctx)
    assertEquals(ctx.obligations.inferredType(application), Type.Bool)
  }

  test("`visitInfixApplication` adds an apply constraint with a known function (3pts)") {
    val f = ast.Identifier("%", dummySite)
    val application = ast.InfixApplication(f, ast.IntegerLiteral("1", dummySite), ast.IntegerLiteral("2", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitInfixApplication(application)(using ctx)
    assert(
      ctx.obligations.constraints.exists(p =>
        p match
          case Constraint.Apply(
              Type.Arrow(List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int)), Type.Int),
              List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int)),
              Type.Int,
              _
            ) => true
          case _ => false
      )
    , f"""Could not find the following constraint:\nConstraint.Apply(
  Type.Arrow(List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int)), Type.Int),
  List(Type.Labeled(None, Type.Int), Type.Labeled(None, Type.Int)),
  Type.Int,
  _
)
            \nHere is the list of all constraints:\n${ctx.obligations.constraints.mkString("\n")}""")
  }

  test("`visitInfixApplication` adds a constraint on the output type of the application (3pts)") {
    shouldPassTypeCheck("let main: Int = 1 + 2")
  }

  test("`visitConditional` should check that the condition is a boolean (3pts)") {
    shouldFailTypeCheck("let main = if \"hello world!\" then 1 else 2")
  }

  test("`visitConditional` should put an output type constraint on the conditional (3pts)") {
    val conditional = ast.Conditional(ast.BooleanLiteral("true", dummySite), ast.IntegerLiteral("1", dummySite), ast.IntegerLiteral("2", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitConditional(conditional)(using ctx)
    assertEquals(ctx.obligations.inferredType(conditional), Type.Int)
  }

  test("`visitLet` should put a constraint on the output (3pts)") {
    // Here, the binding type-checking is provided so it ensures that the type of the let is the same as the ascription of the binding
    shouldPassTypeCheck("""
      let something: Int = let x = 1 { x }
      let main = something
    """.stripMargin) 
  }

  test("`visitLambda` should work as intended (3pts)") {
    shouldPassTypeCheck("""
      let something: (Int) -> Int = (_ x: Int) { x }
    """.stripMargin)
  }

  test("`visitParenthesizedExpression` should add a constraint on the output type (3pts)") {
    val parenthesized = ast.ParenthesizedExpression(ast.IntegerLiteral("1", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitParenthesizedExpression(parenthesized)(using ctx)
    assertEquals(ctx.obligations.inferredType(parenthesized), Type.Int)
  }

  test("`visitAscribedExpression` should check if subtype for widening (3pts)") {
    // checks that 1 is of type Int
    shouldPassTypeCheck("""
      let something: Int = 1 @ Int
    """.stripMargin)
    // checks that 1 is a subtype of Any
    shouldPassTypeCheck("""
      let something: Any = 1 @ Any
    """.stripMargin)
    // if not subtype, should fail
    shouldFailTypeCheck("""
      let something: Int = 1 @ Float
    """.stripMargin)
  }

  test("`visitAscribedExpression` should add a constraint that the type is optional if narrowing (3pts)") {
    shouldPassTypeCheck("""
      let something: #none | #some(Int) = 1 @? Int
    """.stripMargin)
  }

  test("`visitAscribedExpression` should fail if not convertible if narrowing (3pts)") {
    shouldFailTypeCheck("""
      let something: #none | #some(Float) = 1 @? Float
    """.stripMargin)
  }

  test("`visitAscribedExpression` should fail if not convertible if narrowing unconditionally (3pts)") {
    shouldFailTypeCheck("""
      let something: #none | #some(Float) = 1 @! Float
    """.stripMargin)
  }

  test("`visitTypeIdentifier` should return the correct type of a type identifier (3pts)") {
    val typeIdentifier = ast.TypeIdentifier("Int", dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitTypeIdentifier(typeIdentifier)(using ctx)
    assertEquals(result, Type.Int)
  }

  test("`visitArrow` should return the correct type of an arrow (3pts)") {
    val arrow = ast.Arrow(List(ast.Labeled(None, ast.TypeIdentifier("Int", dummySite), dummySite)), ast.TypeIdentifier("Int", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitArrow(arrow)(using ctx)
    assertEquals(result, Type.Arrow(List(Type.Labeled(None, Type.Int)), Type.Int))
  }

  test("`visitArrow` should return the correct type of an arrow (2) (3pts)") {
    val arrow = ast.Arrow(List(ast.Labeled(None, ast.TypeIdentifier("Int", dummySite), dummySite), ast.Labeled(Some("other"), ast.TypeIdentifier("String", dummySite), dummySite)), ast.TypeIdentifier("Int", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitArrow(arrow)(using ctx)
    assertEquals(result, Type.Arrow(List(Type.Labeled(None, Type.Int), Type.Labeled(Some("other"), Type.String)), Type.Int))
  }

  test("`visitParenthesizedType` should return the type that is inside the parentheses (3pts)") {
    val parenthesized = ast.ParenthesizedType(ast.TypeIdentifier("Int", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitParenthesizedType(parenthesized)(using ctx)
    assertEquals(result, Type.Int)
  }

  test("`visitRecordPattern` should return the correct type of a #record(1) (3pts)") {
    val recordPattern = ast.RecordPattern("record", List(ast.Labeled(None, ast.ValuePattern(ast.IntegerLiteral("1", dummySite), dummySite), dummySite)), dummySite)
    val typer = Typer()
    val ctx = Typer.Context()
    val result = typer.visitRecordPattern(recordPattern)(using ctx)
    assertEquals(result, Type.Record("record", List(Type.Labeled(None, Type.Int)))
    )
  }
  
  test("A wildcard pattern can take any type (3pts)") {
    shouldPassTypeCheck("let main = match 1 { case _ then 2 }")
    shouldPassTypeCheck("let main = match \"hi\" { case _ then 2 }")
  }

  test("`visitFunction` should return the correct type of a function (3pts)") {
    val function = ast.Function("main", Nil, List(ast.Parameter(None, "x", Some(ast.TypeIdentifier("Int", dummySite)), dummySite)), Some(ast.TypeIdentifier("Int", dummySite)), ast.IntegerLiteral("1", dummySite), dummySite)
    val typer = Typer()
    typer.syntax = dummyProgram
    val ctx = Typer.Context()
    val result = typer.visitFunction(function)(using ctx)
    assertEquals(result, Type.Arrow(List(Type.Labeled(None, Type.Int)), Type.Int))
  }

  test("`visitSelection` should typecheck a selection with an identifier selectee (3pts)") {
    shouldPassTypeCheck("let main = #a(a: 1).a")
  }

  test("`visitSelection` should typecheck a selection with an integer selectee (3pts)") {
    shouldPassTypeCheck("let main = #a(a: 1).0")
  }

  test("General test 1 (2pts)") {
    shouldPassTypeCheck("let main = 1")
    shouldFailTypeCheck("let main: Int = 1.2")
  }

  test("General test 2 (playing with simple records) (2pts)") {
    shouldPassTypeCheck("""
      let something = #a(4)
      let another: Int = something.0
      let main = another
      """.stripMargin)
  }

  test("General test 2 (gcd function) (2pts)") {
    shouldPassTypeCheck("""
    fun gcd(_ a: Int, _ b: Int) -> Int {
      if b == 0 then a else gcd(b, a % b)
    }
      """.stripMargin)
  }

  test("General test 3 (overloading) (2pts)") {
    shouldPassTypeCheck("""
      fun f(_ x: String) -> String { "hello" }
      fun f(_ x: Int) -> Int { x }
      let overload1: String = f("hello")
      let overload2: Int = f(42)
    """.stripMargin)
  } 

  test("General test 4 (overloading, bis) (2pts)") {
    shouldPassTypeCheck("""
      fun f(_ x: String) -> String { "hello" }
      fun f(_ x: Int) -> Int { x }
      let overload1 = f("hello")
      let overload2 = f(42)
    """.stripMargin)
  }

  test("General test 5 (overloading on binary operators) (2pts)") {
    shouldPassTypeCheck("""
      let overload1 = 1.2 + 1.3
      let x: Float = overload1
      let overload2 = 1 + 2
      let y: Int = overload2
    """.stripMargin)
  }

  test("General test 6 (overloading on unary operators) (2pts)") {
    shouldPassTypeCheck("""
      let overload1 = -1.2
      let x: Float = overload1
      let overload2 = -5
      let y: Int = overload2
    """.stripMargin)
  }

  test("Default return type of function is unit (2pts)") {
    // This should be taken care of by computedUncheckedType
    shouldPassTypeCheck("""
    fun f() { #unit }
    let refToF: () -> #unit = f
    """.stripMargin)

    shouldFailTypeCheck("""
    fun f() { 1 }
    let refToF: () -> #unit = f
    """.stripMargin)
  }

  test("General test 7 (conditionals) (2pts)") {
    shouldPassTypeCheck("""
    let x = if true then 1 else 1.2
    let y: Any = x
    """.stripMargin)

    shouldPassTypeCheck("""
    let x: Int = if true then (if true then 1 else 2) else 3
    """.stripMargin)
  }

  test("General test 8 (conditionals, bis) (2pts)") {
    shouldFailTypeCheck("""
    let x: Int = if true then (if true then 1 else 2.2) else 3
    """.stripMargin)
  }

  test("General test 9 (sum types) (2pts)") {
    shouldPassTypeCheck("""
    let x: #a(a:Int) | #a = #a
    let y: #a(a:Int) | #a = #a(a:4)
    """.stripMargin)

    shouldFailTypeCheck("""
    let x: #a(a:Int) | #a = #a
    let y: #a(a:Int) | #a = #a(4)
    """.stripMargin)
  }

  test("General test 10 (sum types, bis) (2pts)") {
    shouldPassTypeCheck("""
    let x: #a(a:Int) | #a = #a
    let y: #a(a:Int) | #a = #a(a:4)
    let z: #a(a:Int) | #a = #a(a:4)
    """.stripMargin)

    shouldFailTypeCheck("""
    let x: #a(a:Int) | #a = #a
    let y: #a(a:Int) | #a = #a(a:4)
    let z: #a(a:Int) | #a = #a(a:4.2)
    """.stripMargin)
  }

  test("General test 11 (applications) (2pts)") {
    shouldPassTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    let x: Int = f(1)
    """.stripMargin)

    shouldFailTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    let x: Int = f(1.2)
    """.stripMargin)
  }

  test("General test 12 (applications, bis) (2pts)") {
    shouldPassTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    let x: Int = f(1)
    let y: Int = f(2)
    """.stripMargin)

    shouldFailTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    let x: Int = f(1.2)
    let y: Int = f(2)
    """.stripMargin)
  }

  test("General test 13 (overloading functions) (2pts)") {
    shouldPassTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    fun f(_ x: Float) -> Float { x }
    let x: Int = f(1)
    let y: Float = f(1.2)
    """.stripMargin)

    shouldFailTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    fun f(_ x: Float) -> Float { x }
    let x: Int = f(1.2)
    let y: Float = f(1)
    """.stripMargin)

    shouldPassTypeCheck("""
    fun f(_ x: Int) -> Float { 1.1 }
    fun f(_ x: Float) -> Int { 0 }
    let x: Int = f(1.2)
    let y: Float = f(1)
    """.stripMargin)
  }

  test("General test 14 (incompatible return type) (2pts)") {
    shouldFailTypeCheck("""
    fun f(_ x: Int) -> Int { x }
    fun g(_ x: Int) -> Float { x }
    let x: Int = f(1)
    let y: Int = g(1)
    """.stripMargin)
  }

  test("General test 15 (selections) (2pts)") {
    shouldPassTypeCheck("""
    let x = #a(a: 1).a
    let y: Int = x
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = #a(a: 1).a
    let y: Float = x
    """.stripMargin)
  }

  test("General test 16 (selections, bis) (2pts)") {
    shouldPassTypeCheck("""
    let x = #a(a: 1).0
    let y: Int = x
    let z: Int = #a(a: 1).a
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = #a(a: 1).0
    let y: Float = x
    let z: Int = #a(a: 1.2).a
    """.stripMargin)
  }

  // pattern matching
  test("General test 17 (pattern matching) (2pts)") {
    shouldPassTypeCheck("""
    let x = match #a(a: 1) { case #a(a: let x) then x }
    let y: Int = x
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = match #a(a: 1) { case #a(a: let x) then x }
    let y: Float = x
    """.stripMargin)
  }

  test("General test 18 (pattern matching, bis) (2pts)") {
    shouldPassTypeCheck("""
    let x = match #a(a: 1) { case #a(a: let x) then x }
    let y: Int = x
    let z: Int = match #a(a: 1) { case #a(a: let x) then x }
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = match #a(a: 1) { case #a(a: let x) then x }
    let y: Float = x
    let z: Int = match #a(a: 1.2) { case #a(a: let x) then x }
    """.stripMargin)
  }

  test("General test 19 (pattern matching on values) (2pts)") {
    shouldPassTypeCheck("""
    let x = match 1 { case 1 then 1 }
    let y: Int = x
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = match 1 { case 1 then 1 }
    let y: Float = x
    """.stripMargin)
  }

  test("General test 20 (pattern matching with invalid patterns) (2pts)") {
    shouldFailTypeCheck("""
    let x = match 1 { case 1.2 then 1 }
    let y: Int = x
    """.stripMargin)

    shouldFailTypeCheck("""
    let x = match 1 { case #a(x: Int) then 1 }
    let y: Int = x
    """.stripMargin)
  }

  test("General test 21 (pattern matching with wildcard) (2pts)") {
    shouldPassTypeCheck("""
    let x = match 1 { case _ then 1 }
    let y: Int = x
    """.stripMargin)

    shouldPassTypeCheck("""
    let x = match "hi" { case _ then 1 }
    let y: Int = x
    """.stripMargin)
  }

  def shouldFailTypeCheck(code: String) =
    val source = SourceFile("dummy", code)
    val parser = parsing.Parser(source)
    val syntax = parser.program()
    val ds = parser.diagnostics
    ds.throwOnError()
    ds.log()
    val typer = Typer()
    typer.syntax = syntax
    val typedSyntax = typer.check(syntax)
    val ds2 = typer.diagnostics
    assert(ds2.containsError, "assertion failed, expected errors")

  def shouldPassTypeCheck(code: String) =
    val source = SourceFile("dummy", code)
    val parser = parsing.Parser(source)
    val syntax = parser.program()
    val ds = parser.diagnostics
    ds.throwOnError()
    ds.log()
    val typer = Typer()
    typer.syntax = syntax
    val typedSyntax = typer.check(syntax)
    val ds2 = typer.diagnostics
    assert(!ds2.containsError, f"assertion failed, expected no errors but got ${ds2.elements.mkString("\n")}")
    ds2.throwOnError()
    ds2.log()

  extension[T](v: scala.collection.View[T])
    def contains(t: T): Boolean = v.exists(_ == t)

end TyperTests
