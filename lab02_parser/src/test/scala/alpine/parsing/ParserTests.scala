package alpine.parsing

import alpine.ast._
import alpine.parsing.{Parser, Token}
import alpine.SourceFile
import alpine.parsing.Lexer


class ParserTests extends munit.FunSuite:

  import Token.Kind as K

  // Primary expressions

  test("`primaryExpression` parses identifiers (0pt)") {
    val p = instrumentParser("identifier", 0)
    p.primaryExpression() match
      case Identifier("identifier", _) => ()
      case x => fail(f"Expected an identifier, got $x.")
  }

  test("`primaryExpression` parses booleans (0pt)") {
    val p = instrumentParser("true false", 0)
    p.primaryExpression() match
      case BooleanLiteral("true", _) => ()
      case x => fail(f"Expected a true boolean lit., got $x.")
    p.primaryExpression() match
      case BooleanLiteral("false", _) => ()
      case x => fail(f"Expected a false boolean literal, got $x.")
  }

  test("`primaryExpression` parses integers (0pt)") {
    val p = instrumentParser("123", 0)
    p.primaryExpression() match
      case IntegerLiteral("123", _) => ()
      case x => fail(f"Expected an integer literal, got $x.")
  }

  test("`primaryExpression` parses floats (0pt)") {
    val p = instrumentParser("123.", 0)
    p.primaryExpression() match
      case FloatLiteral("123.", _) => ()
      case x => fail(f"Expected an float literal, got $x.")
  }

  test("`primaryExpression` parses strings (0pt)") {
    val p = instrumentParser(""""I'm a string :)"""", 0)
    p.primaryExpression() match
      case StringLiteral(""""I'm a string :)"""", _) => ()
      case x => fail(f"Expected a string literal, got $x.")
  }

  test("`primaryExpression` parses a singleton record (1pt)") {
    val p = instrumentParser("#singleton", 0)
    p.primaryExpression() match
      case Record("#singleton", List(), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a record without labels and single argument (1pt)") {
    val p = instrumentParser("#value(2)", 0)
    p.primaryExpression() match
      case Record("#value", List(Labeled(None, IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a record with a labelled single argument  (1pt)") {
    val p = instrumentParser("#value(label: 2)", 0)
    p.primaryExpression() match
      case Record("#value", List(Labeled(Some("label"), IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a record with multiple labelled arguments (1pt)") {
    val p = instrumentParser("#value(label1: 2, label2: 3)", 0)
    p.primaryExpression() match
      case Record("#value", List(Labeled(Some("label1"), IntegerLiteral("2", _), _), Labeled(Some("label2"), IntegerLiteral("3", _), _)), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a record with multiple labelled and unlabelled arguments (1pt)") {
    val p = instrumentParser("#value(2, label2: 3)", 0)
    p.primaryExpression() match
      case Record("#value", List(Labeled(None, IntegerLiteral("2", _), _), Labeled(Some("label2"), IntegerLiteral("3", _), _)), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a record with multiple unlabelled arguments (1pt)") {
    val p = instrumentParser("#value(2, 3)", 0)
    p.primaryExpression() match
      case Record("#value", List(Labeled(None, IntegerLiteral("2", _), _), Labeled(None, IntegerLiteral("3", _), _)), _) => ()
      case x => fail(f"Expected a singleton, got $x.")
  }

  test("`primaryExpression` parses a conditional expression (1pt)") {
    val p = instrumentParser("if true then 1 else 2", 0)
    p.primaryExpression() match
      case Conditional(BooleanLiteral("true", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _) => ()
      case x => fail(f"Expected a conditional expression, got $x.")
  }

  test("`primaryExpression` parses a match expression (1pt)") {
    val p = instrumentParser("match 1 { case 1 then 2 case 2 then 3 }", 0)
    p.primaryExpression() match
      case Match(IntegerLiteral("1", _), List(Match.Case(ValuePattern(IntegerLiteral("1", _), _), IntegerLiteral("2", _), _), Match.Case(ValuePattern(IntegerLiteral("2", _), _), IntegerLiteral("3", _), _)), _) => ()
      case x => fail(f"Expected a match expression, got $x.")
  }

  test("`primaryExpression` parses a let expression (1pt)") {
    val p = instrumentParser("let x = 1 { x }", 0)
    p.primaryExpression() match
      case Let(Binding("x", None, Some(IntegerLiteral("1", _)), _),  Identifier("x", _), _) => ()
      case x => fail(f"Expected a let expression, got $x.")
  }

  test("`primaryExpression` parses a parenthesized expression (1pt)") {
    val p = instrumentParser("(50)", 0)
    p.primaryExpression() match
      case ParenthesizedExpression(IntegerLiteral("50", _), _) => ()
      case x => fail(f"Expected a parenthesized expression, got $x.")
  }

  test("`primaryExpression` parses a lambda function (1pt)") {
    val p = instrumentParser("() { 50 }", 0)
    p.primaryExpression() match
      case Lambda(List(), None, IntegerLiteral("50", _), _) => ()
      case x => fail(f"Expected a lambda function, got $x.")
  }

  test("`primaryExpression` parses an operator (0pt)") {
    val p = instrumentParser("+", 0)
    p.primaryExpression() match
      case Identifier("+", _) => ()
      case x => fail(f"Expected a binary operation, got $x.")
  }

  // Compound expression

  test("`compoundExpression` can parse a selection with an int (1pt)") {
    val p = instrumentParser("someValue.2", 0)
    p.compoundExpression() match
      case Selection(Identifier("someValue", _), IntegerLiteral("2", _), _) => ()
      case x => fail(f"Expected a selection, got $x.")
  }

  test("`compoundExpression` can parse a selection with an int (1pt)") {
    val p = instrumentParser("someValue.another", 0)
    p.compoundExpression() match
      case Selection(Identifier("someValue", _), Identifier("another", _), _) => ()
      case x => fail(f"Expected a selection, got $x.")
  }

  test("`compoundExpression` can parse an operator call (1pt)") {
    val p = instrumentParser("someValue.+", 0)
    p.compoundExpression() match
      case Selection(Identifier("someValue", _), Identifier("+", _), _) => ()
      case x => fail(f"Expected a selection, got $x.")
  }

  test("`compoundExpression` can parse a function call (1pt)") {
    val p = instrumentParser("someValue(1)", 0)
    p.compoundExpression() match
      case Application(Identifier("someValue", _), List(Labeled(None, IntegerLiteral("1", _), _)), _) => ()
      case x => fail(f"Expected a function call with correct arguments, got $x.")
  }

  test("`compoundExpression` can parse a function call with a label (1pt)") {
    val p = instrumentParser("someValue(label: 1)", 0)
    p.compoundExpression() match
      case Application(Identifier("someValue", _), List(Labeled(Some("label"), IntegerLiteral("1", _), _)), _) => ()
      case x => fail(f"Expected a function call with correct arguments, got $x.")
  }

  test("`compoundExpression` can parse a function call with multiple arguments (1pt)") {
    val p = instrumentParser("someValue(1, label: 2)", 0)
    p.compoundExpression() match
      case Application(Identifier("someValue", _), List(Labeled(None, IntegerLiteral("1", _), _), Labeled(Some("label"), IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a function call with correct arguments, got $x.")
  }

  // Prefix expression

  test("`prefixExpression` can parse a - operator (1pt)") {
    val p = instrumentParser("-1", 0)
    p.prefixExpression() match
      case PrefixApplication(Identifier("-", _), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a unary operation with correct fields, got $x.")
  }

  test("`prefixExpression` can parse a ! operator (1pt)") {
    val p = instrumentParser("!true", 0)
    p.prefixExpression() match
      case PrefixApplication(Identifier("!", _), BooleanLiteral("true", _), _) => ()
      case x => fail(f"Expected a unary operation with correct fields, got $x.")
  }

  test("`prefixExpression` can parse a ~ operator (1pt)") {
    val p = instrumentParser("~1", 0)
    p.prefixExpression() match
      case PrefixApplication(Identifier("~", _), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a unary operation with correct fields, got $x.")
  }

  test("`prefixExpression` does not parse a + and a literal if there is a space in between (1pt)") {
    val p = instrumentParser("+ 1", 0)
    p.prefixExpression() match
      case Identifier("+", _) => ()
      case x => fail(f"Expected an operator, got $x.")
  }

  test("`prefixExpression` takes from a `compoundExpression (1pt)`") {
    val p = instrumentParser("-someValue.something", 0)
    p.prefixExpression() match
      case PrefixApplication(Identifier("-", _), Selection(Identifier("someValue", _), Identifier("something", _), _), _) => ()
      case x => fail(f"Expected a unary operation with correct fields, got $x.")
  }

  // Ascribed expression

  test("`ascribed` can parse a widening type ascription (1pt)") {
    val p = instrumentParser("1 @ Int", 0)
    p.ascribed() match
      case AscribedExpression(IntegerLiteral("1", _), Typecast.Widen, TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected an ascription with correct fields, got $x.")
  }

  test("`ascribed` can parse a unconditional type ascription (1pt)") {
    val p = instrumentParser("1 @! Int", 0)
    p.ascribed() match
      case AscribedExpression(IntegerLiteral("1", _), Typecast.NarrowUnconditionally, TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected an ascription with correct fields, got $x.")
  }

  test("`ascribed` can parse a narrowing type ascription (1pt)") {
    val p = instrumentParser("1 @? Int", 0)
    p.ascribed() match
      case AscribedExpression(IntegerLiteral("1", _), Typecast.Narrow, TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected an ascription with correct fields, got $x.")
  }

  test("`ascribed` takes from `prefixExpression (1pt)`") {
    val p = instrumentParser("-1 @ Int", 0)
    p.ascribed() match
      case AscribedExpression(PrefixApplication(Identifier("-", _), IntegerLiteral("1", _), _), Typecast.Widen, TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected a correct ascription, got $x.")
  }

  // Infix expression

  test("`infixExpression` can parse a binary operation (1pt)") {
    val p = instrumentParser("1 + 2", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("+", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }

  test("`infixExpression` can parse multiple operations with the same precedence (1pt)") {
    val p = instrumentParser("1 + 2 + 3", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("+", _), InfixApplication(Identifier("+", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _), IntegerLiteral("3", _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }

  test("`infixExpression` can parse multiple operations with different precedence (1pt)") {
    val p = instrumentParser("1 + 2 * 3", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("+", _), IntegerLiteral("1", _), InfixApplication(Identifier("*", _), IntegerLiteral("2", _), IntegerLiteral("3", _), _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }

  test("`infixExpression` can parse multiple operations with different precedence (1pt)") {
    val p = instrumentParser("1 * 2 + 3", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("+", _), InfixApplication(Identifier("*", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _), IntegerLiteral("3", _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }

  test("`infixExpression` can parse multiple operations with same precedence (1pt)") {
    val p = instrumentParser("1 * 2 * 3", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("*", _), InfixApplication(Identifier("*", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _), IntegerLiteral("3", _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }

  test("`infixExpression` takes from ascribed (1pt)") {
    val p = instrumentParser("1 @ Int + 2", 0)
    p.infixExpression() match
      case InfixApplication(Identifier("+", _), AscribedExpression(IntegerLiteral("1", _), Typecast.Widen, TypeIdentifier("Int", _), _), IntegerLiteral("2", _), _) => ()
      case x => fail(f"Expected a correct binary operation, got $x.")
  }
  
  // Labeled 
  test("`labeled` works with expressions and a label (1pt)") {
    val p = instrumentParser("label: 1", 0)
    p.labeled(p.expression) match
      case Labeled(Some("label"), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct labeled expression, got $x.")
  }

  test("`labeled` works with expressions and an unlabelled expression (1pt)") {
    val p = instrumentParser("1", 0)
    p.labeled(p.expression) match
      case Labeled(None, IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct labeled expression, got $x.")
  }

  test("`labeled` works with expressions and a label and an unlabelled expression (1pt)") {
    val p = instrumentParser("label: 1, 2", 0)
    p.labeled(p.expression) match
      case Labeled(Some("label"), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct labeled expression, got $x.")
  }

  test("`labeled` works with keyword as a label (1pt)") {
    val p = instrumentParser("match: 1, 2", 0)
    p.labeled(p.expression) match
      case Labeled(Some("match"), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct labeled expression, got $x.")
  }

    test("`labeled` works with keyword as a label, bis (1pt)") {
    val p = instrumentParser("fun: 1, 2", 0)
    p.labeled(p.expression) match
      case Labeled(Some("fun"), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct labeled expression, got $x.")
  }

  // inParentheses, inBraces, inAngles
  test("`inParentheses` works with a single expression (1pt)") {
    val p = instrumentParser("(1)", 0)
    p.inParentheses(p.integerLiteral) match
      case IntegerLiteral("1", _) => ()
      case x => fail(f"Expected a correct parenthesized expression, got $x.")
  }

  test("`inBraces` works with a single integer (1pt)") {
    val p = instrumentParser("{1}", 0)
    p.inBraces(p.integerLiteral) match
      case IntegerLiteral("1", _) => ()
      case x => fail(f"Expected a correct braced expression, got $x.")
  }

  test("`inAngles` works with a single integer (1pt)") {
    val p = instrumentParser("<1>", 0)
    p.inAngles(p.integerLiteral) match
      case IntegerLiteral("1", _) => ()
      case x => fail(f"Expected a correct angled expression, got $x.")
  }

  test("`inParentheses` works with expressions (1pt)") {
    val p = instrumentParser("(1 + 2)", 0)
    p.inParentheses(p.expression) match
      case InfixApplication(Identifier("+", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _) => ()
      case x => fail(f"Expected a correct parenthesized expression, got $x.")
  }

  // Parenthesized labeled list
  test("`parenthesizedLabeledList` works with a single expression (1pt)") {
    val p = instrumentParser("(1)", 0)
    p.parenthesizedLabeledList(p.integerLiteral) match
      case List(Labeled(None, IntegerLiteral("1", _), _)) => ()
      case x => fail(f"Expected a correct parenthesized labeled list, got $x.")
  }

  test("`parenthesizedLabeledList` works with a single labeled expression (1pt)") {
    val p = instrumentParser("(label: 1)", 0)
    p.parenthesizedLabeledList(p.integerLiteral) match
      case List(Labeled(Some("label"), IntegerLiteral("1", _), _)) => ()
      case x => fail(f"Expected a correct parenthesized labeled list, got $x.")
  }

  test("`parenthesizedLabeledList` works with a single labeled expression and an unlabelled expression (1pt)") {
    val p = instrumentParser("(label: 1, 2)", 0)
    p.parenthesizedLabeledList(p.integerLiteral) match
      case List(Labeled(Some("label"), IntegerLiteral("1", _), _), Labeled(None, IntegerLiteral("2", _), _)) => ()
      case x => fail(f"Expected a correct parenthesized labeled list, got $x.")
  }

  test("`parenthesizedLabeledList` works with no elements (1pt)") {
    val p = instrumentParser("()", 0)
    p.parenthesizedLabeledList(p.integerLiteral) match
      case List() => ()
      case x => fail(f"Expected a correct parenthesized labeled list, got $x.")
  }
    
  // Types
  test("`tpe` works with a single type identifier (1pt)") {
    val p = instrumentParser("Int", 0)
    p.tpe() match
      case TypeIdentifier("Int", _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`tpe` works with a sum of types (1pt)") {
    val p = instrumentParser("Int | Float", 0)
    p.tpe() match
      case Sum(List(TypeIdentifier("Int", _), TypeIdentifier("Float", _)), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`tpe` works with a sum of types (1pt)") {
    val p = instrumentParser("Int | Float | AnotherType", 0)
    p.tpe() match
      case Sum(List(TypeIdentifier("Int", _), TypeIdentifier("Float", _), TypeIdentifier("AnotherType", _)), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  // Record types
  test("`recordType` works with a singleton record type (1pt)") {
    val p = instrumentParser("#record", 0)
    p.recordType() match
      case RecordType("#record", List(), _) => ()
      case x => fail(f"Expected a correct record type, got $x.")
  }

  test("`recordType` works with a singleton containing a single labelled type (1pt)") {
    val p = instrumentParser("#record(Int)", 0)
    p.recordType() match
      case RecordType("#record", List(Labeled(None, TypeIdentifier("Int", _), _)), _) => ()
      case x => fail(f"Expected a correct record type, got $x.")
  }

  test("`recordType` works with a singleton containing a single non-labelled type (1pt)") {
    val p = instrumentParser("#record(a: Int)", 0)
    p.recordType() match
      case RecordType("#record", List(Labeled(Some("a"), TypeIdentifier("Int", _), _)), _) => ()
      case x => fail(f"Expected a correct record type, got $x.")
  }

  test("`recordType` works with a singleton multiple labelled types (1pt)") {
    val p = instrumentParser("#record(a: Int, b: Float)", 0)
    p.recordType() match
      case RecordType("#record", List(Labeled(Some("a"), TypeIdentifier("Int", _), _), Labeled(Some("b"), TypeIdentifier("Float", _), _)), _) => ()
      case x => fail(f"Expected a correct record type, got $x.")
  }

  test("`recordType` works with a singleton multiple labelled and non-labelled types (1pt)") {
    val p = instrumentParser("#record(Int, b: Float)", 0)
    p.recordType() match
      case RecordType("#record", List(Labeled(None, TypeIdentifier("Int", _), _), Labeled(Some("b"), TypeIdentifier("Float", _), _)), _) => ()
      case x => fail(f"Expected a correct record type, got $x.")
  }

  // Arrow or parenthesized types
  test("`arrowOrParenthesizedType` works with a single type (1pt)") {
    val p = instrumentParser("(Int)", 0)
    p.arrowOrParenthesizedType() match
      case ParenthesizedType(TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`arrowOrParenthesizedType` works with arrow/lambda functions and single unlabelled argument (1pt)") {
    val p = instrumentParser("(Int) -> Float", 0)
    p.arrowOrParenthesizedType() match
      case Arrow(List(Labeled(None, TypeIdentifier("Int", _), _)), TypeIdentifier("Float", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`arrowOrParenthesizedType` works with arrow/lambda functions and single labelled argument (1pt)") {
    val p = instrumentParser("(a: Int) -> Float", 0)
    p.arrowOrParenthesizedType() match
      case Arrow(List(Labeled(Some("a"), TypeIdentifier("Int", _), _)), TypeIdentifier("Float", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`arrowOrParenthesizedType` works with arrow/lambda functions and multiple labelled and unlabelled arguments (1pt)") {
    val p = instrumentParser("(Int, a: Float) -> Float", 0)
    p.arrowOrParenthesizedType() match
      case Arrow(List(Labeled(None, TypeIdentifier("Int", _), _), Labeled(Some("a"), TypeIdentifier("Float", _), _)), TypeIdentifier("Float", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`arrowOrParenthesizedType` works with arrow/lambda functions and multiple labelled arguments (1pt)") {
    val p = instrumentParser("(a: Int, b: Float) -> Float", 0)
    p.arrowOrParenthesizedType() match
      case Arrow(List(Labeled(Some("a"), TypeIdentifier("Int", _), _), Labeled(Some("b"), TypeIdentifier("Float", _), _)), TypeIdentifier("Float", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  test("`arrowOrParenthesizedType` works with arrow/lambda functions and multiple unlabelled arguments (1pt)") {
    val p = instrumentParser("(Int, Float) -> Float", 0)
    p.arrowOrParenthesizedType() match
      case Arrow(List(Labeled(None, TypeIdentifier("Int", _), _), Labeled(None, TypeIdentifier("Float", _), _)), TypeIdentifier("Float", _), _) => ()
      case x => fail(f"Expected a correct type, got $x.")
  }

  // Type Declaration
  test("`typeDeclaration` works with a single type (1pt)") {
    val p = instrumentParser("type T = Int", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), TypeIdentifier("Int", _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a sum of types (1pt)") {
    val p = instrumentParser("type T = Int | Float", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), Sum(List(TypeIdentifier("Int", _), TypeIdentifier("Float", _)), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a record type (1pt)") {
    val p = instrumentParser("type T = #record", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), RecordType("#record", List(), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a record type with a single labelled type (1pt)") {
    val p = instrumentParser("type T = #record(a: Int)", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), RecordType("#record", List(Labeled(Some("a"), TypeIdentifier("Int", _), _)), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a record type with multiple labelled types (1pt)") {
    val p = instrumentParser("type T = #record(a: Int, b: Float)", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), RecordType("#record", List(Labeled(Some("a"), TypeIdentifier("Int", _), _), Labeled(Some("b"), TypeIdentifier("Float", _), _)), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a record type with multiple labelled and unlabelled types (1pt)") {
    val p = instrumentParser("type T = #record(Int, b: Float)", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), RecordType("#record", List(Labeled(None, TypeIdentifier("Int", _), _), Labeled(Some("b"), TypeIdentifier("Float", _), _)), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with a parenthesized type (1pt)") {
    val p = instrumentParser("type T = (Int)", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), ParenthesizedType(TypeIdentifier("Int", _), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with an arrow/lambda function type (1pt)") {
    val p = instrumentParser("type T = (Int) -> Float", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), Arrow(List(Labeled(None, TypeIdentifier("Int", _), _)), TypeIdentifier("Float", _), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  test("`typeDeclaration` works with an arrow/lambda function type with multiple labelled and unlabelled arguments (1pt)") {
    val p = instrumentParser("type T = (Int, a: Float) -> Float", 0)
    p.typeDeclaration() match
      case TypeDeclaration("T", List(), Arrow(List(Labeled(None, TypeIdentifier("Int", _), _), Labeled(Some("a"), TypeIdentifier("Float", _), _)), TypeIdentifier("Float", _), _), _) => ()
      case x => fail(f"Expected a correct type declaration, got $x.")
  }

  // Bindings
  test("`binding` works with no initializer when passed `initializerIsExpected = false (1pt)`") {
    val p = instrumentParser("let x", 0)
    p.binding(false) match
      case Binding("x", None, None, _) => ()
      case x => fail(f"Expected a correct binding, got $x.")
    assert(p.snapshot().errorCount == 0)
  }

  test("`binding` reports when no initializer is found when passed `initializerIsExpected = true (1pt)`") {
    val p = instrumentParser("let x", 0)
    p.binding()
    assert(p.snapshot().errorCount > 0)
  }

  test("`binding` works with an initializer (1pt)") {
    val p = instrumentParser("let x = 1", 0)
    p.binding() match
      case Binding("x", None, Some(IntegerLiteral("1", _)), _) => ()
      case x => fail(f"Expected a correct binding, got $x.")
  }

  test("`binding` works with a type and no initializer when passed `initializerIsExpected = false (1pt)`") {
    val p = instrumentParser("let x: Int", 0)
    p.binding(false) match
      case Binding("x", Some(TypeIdentifier("Int", _)), None, _) => ()
      case x => fail(f"Expected a correct binding, got $x.")
  }

  test("`binding` reports when no initializer is found when passed `initializerIsExpected = true (1pt)`") {
    val p = instrumentParser("let x: Int", 0)
    p.binding()
    assert(p.snapshot().errorCount > 0)
  }

  test("`binding` works with a type and an initializer (1pt)") {
    val p = instrumentParser("let x: Int = 1", 0)
    p.binding() match
      case Binding("x", Some(TypeIdentifier("Int", _)), Some(IntegerLiteral("1", _)), _) => ()
      case x => fail(f"Expected a correct binding, got $x.")
  }

  test("`binding` uses `expression (1pt)`") {
    val p = instrumentParser("let x = 1 + 2", 0)
    p.binding() match
      case Binding("x", None, Some(InfixApplication(Identifier("+", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a correct binding, got $x.")
  }

  // Let
  test("`let` works with a single binding (1pt)") {
    val p = instrumentParser("let x = 1 { x }", 0)
    p.let() match
      case Let(Binding("x", None, Some(IntegerLiteral("1", _)), _), Identifier("x", _), _) => ()
      case x => fail(f"Expected a correct let expression, got $x.")
  }

  test("`let` works with a binding and a type (1pt)") {
    val p = instrumentParser("let x: Int = 1 { x }", 0)
    p.let() match
      case Let(Binding("x", Some(TypeIdentifier("Int", _)), Some(IntegerLiteral("1", _)), _), Identifier("x", _), _) => ()
      case x => fail(f"Expected a correct let expression, got $x.")
  }

  // Parameter
  test("`parameter` works with a unlabelled typed parameter (1pt)") {
    val p = instrumentParser("_ x: Int", 0)
    p.parameter() match
      case Parameter(None, "x", Some(TypeIdentifier("Int", _)), _) => ()
      case x => fail(f"Expected a correct parameter, got $x.")
  }

  test("`parameter` works with a labelled typed parameter (1pt)") {
    val p = instrumentParser("label x: Int", 0)
    p.parameter() match
      case Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _) => ()
      case x => fail(f"Expected a correct parameter, got $x.")
  }

  test("`parameter` works with a labelled non-typed parameter (1pt)") {
    val p = instrumentParser("label x", 0)
    p.parameter() match
      case Parameter(Some("label"), "x", None, _) => ()
      case x => fail(f"Expected a correct parameter, got $x.")
  }

  test("`parameter` works with a unlabelled non-typed parameter (1pt)") {
    val p = instrumentParser("_ x", 0)
    p.parameter() match
      case Parameter(None, "x", None, _) => ()
      case x => fail(f"Expected a correct parameter, got $x.")
  }

  test("`parameter` works with a label that is a keyword (1pt)") {
    val p = instrumentParser("match x: Int", 0)
    p.parameter() match
      case Parameter(Some("match"), "x", Some(TypeIdentifier("Int", _)), _) => ()
      case x => fail(f"Expected a correct parameter, got $x.")
  }

  // ValueParameterList
  test("`valueParameterList` works with a single unlabelled parameter (1pt)") {
    val p = instrumentParser("(_ x: Int)", 0)
    p.valueParameterList() match
      case List(Parameter(None, "x", Some(TypeIdentifier("Int", _)), _)) => ()
      case x => fail(f"Expected a correct value parameter list, got $x.")
  }

  test("`valueParameterList` works with a single labelled parameter (1pt)") {
    val p = instrumentParser("(label x: Int)", 0)
    p.valueParameterList() match
      case List(Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _)) => ()
      case x => fail(f"Expected a correct value parameter list, got $x.")
  }

  test("`valueParameterList` works with a single labelled parameter and a single unlabelled parameter (1pt)") {
    val p = instrumentParser("(label x: Int, _ y: Float)", 0)
    p.valueParameterList() match
      case List(Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _), Parameter(None, "y", Some(TypeIdentifier("Float", _)), _)) => ()
      case x => fail(f"Expected a correct value parameter list, got $x.")
  }

  test("`valueParameterList` works with a single unlabelled parameter and a single labelled parameter (1pt)") {
    val p = instrumentParser("(_ x: Int, label y: Float)", 0)
    p.valueParameterList() match
      case List(Parameter(None, "x", Some(TypeIdentifier("Int", _)), _), Parameter(Some("label"), "y", Some(TypeIdentifier("Float", _)), _)) => ()
      case x => fail(f"Expected a correct value parameter list, got $x.")
  }

  // Function
  test("`function` works with a function with no arguments (1pt)") {
    val p = instrumentParser("fun f() { 1 }", 0)
    p.function() match
      case Function("f", List(), List(), None, IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  test("`function` works with a function with a single argument (1pt)") {
    val p = instrumentParser("fun f(_ x: Int) { 1 }", 0)
    p.function() match
      case Function("f", List(), List(Parameter(None, "x", Some(TypeIdentifier("Int", _)), _)), None, IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  test("`function` works with a function with a single labelled argument (1pt)") {
    val p = instrumentParser("fun f(label x: Int) { 1 }", 0)
    p.function() match
      case Function("f", List(), List(Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _)), None, IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  test("`function` works with a function with a single labelled argument and a single unlabelled argument (1pt)") {
    val p = instrumentParser("fun f(label x: Int, _ y: Float) { 1 }", 0)
    p.function() match
      case Function("f", List(), List(Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _), Parameter(None, "y", Some(TypeIdentifier("Float", _)), _)), None, IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  test("`function` body is parsed using `expression (1pt)`") {
    val p = instrumentParser("fun f() { 1 + 2 }", 0)
    p.function() match
      case Function("f", List(), List(), None, InfixApplication(Identifier("+", _), IntegerLiteral("1", _), IntegerLiteral("2", _), _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  test("`function` also parses the optional return type (1pt)") {
    val p = instrumentParser("fun f() -> Int { 1 }", 0)
    p.function() match
      case Function("f", List(), List(), Some(TypeIdentifier("Int", _)), IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct function, got $x.")
  }

  // Match
  test("`mtch` parses correctly match with a single case (1pt)") {
    val p = instrumentParser("match 1 { case 1 then 2 }", 0)
    p.mtch() match
      case Match(IntegerLiteral("1", _), List(Match.Case(ValuePattern(IntegerLiteral("1", _), _), IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a correct match expression, got $x.")
  }

  test("`mtch` parses correctly match with different cases (1pt)") {
    val p = instrumentParser("match 1 { case 1 then 2 case 2 then 3 }", 0)
    p.mtch() match
      case Match(IntegerLiteral("1", _), List(Match.Case(ValuePattern(IntegerLiteral("1", _), _), IntegerLiteral("2", _), _), Match.Case(ValuePattern(IntegerLiteral("2", _), _), IntegerLiteral("3", _), _)), _) => ()
      case x => fail(f"Expected a correct match expression, got $x.")
  }

  test("`mtch` parses correctly match with wildcard pattern (1pt)") {
    val p = instrumentParser("match 1 { case _ then 2 }", 0)
    p.mtch() match
      case Match(IntegerLiteral("1", _), List(Match.Case(Wildcard(_), IntegerLiteral("2", _), _)), _) => ()
      case x => fail(f"Expected a correct match expression, got $x.")
  }

  // Pattern
  test("`pattern` parses correctly a value pattern (1pt)") {
    val p = instrumentParser("1", 0)
    p.pattern() match
      case ValuePattern(IntegerLiteral("1", _), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses correctly a wildcard pattern (1pt)") {
    val p = instrumentParser("_", 0)
    p.pattern() match
      case Wildcard(_) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses correctly a binding pattern (1pt)") {
    val p = instrumentParser("let x", 0)
    p.pattern() match
      case Binding("x", None, None, _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses correctly a binding with a type (1pt)") {
    val p = instrumentParser("let x: Int", 0)
    p.pattern() match
      case Binding("x", Some(TypeIdentifier("Int", _)), None, _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` reports an error with a binding with an initializer (1pt)") {
    val p = instrumentParser("let x = 1", 0)
    p.pattern()
    assert(p.snapshot().errorCount > 0)
  }

  test("`pattern` parses singleton record pattern (1pt)") {
    val p = instrumentParser("#record", 0)
    p.pattern() match
      case RecordPattern("#record", List(), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses record pattern with a single labelled pattern (1pt)") {
    val p = instrumentParser("#record(a: 1)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(Some("a"), ValuePattern(IntegerLiteral("1", _), _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses record pattern with multiple labelled patterns (1pt)") {
    val p = instrumentParser("#record(a: 1, b: 2)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(Some("a"), ValuePattern(IntegerLiteral("1", _), _), _), Labeled(Some("b"), ValuePattern(IntegerLiteral("2", _), _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses record pattern with multiple labelled and unlabelled patterns (1pt)") {
    val p = instrumentParser("#record(1, b: 2)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(None, ValuePattern(IntegerLiteral("1", _), _), _), Labeled(Some("b"), ValuePattern(IntegerLiteral("2", _), _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses also bindings (1pt)") {
    val p = instrumentParser("#record(let x)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(None, Binding("x", None, None, _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses also bindings with a type (1pt)") {
    val p = instrumentParser("#record(let x: Int)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(None, Binding("x", Some(TypeIdentifier("Int", _)), None, _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }

  test("`pattern` parses a record with a binding and a value pattern (1pt)") {
    val p = instrumentParser("#record(let x: Int, 3)", 0)
    p.pattern() match
      case RecordPattern("#record", List(Labeled(None, Binding("x", Some(TypeIdentifier("Int", _)), None, _), _), Labeled(None, ValuePattern(IntegerLiteral("3", _), _), _)), _) => ()
      case x => fail(f"Expected a correct pattern, got $x.")
  }


  def instrumentParser(
    code: String,
    lastBoundary: Int,
    lookahead: Option[Token] = None,
    errorCount: Int = 0,
    recoveryPredicateCount: Int = 0) =
    val lexer = Lexer(SourceFile("test", code))
    val parser = Parser(SourceFile("test", code))
    parser.restore(Parser.Snapshot(lexer, lastBoundary, lookahead, errorCount, recoveryPredicateCount))
    parser

end ParserTests
