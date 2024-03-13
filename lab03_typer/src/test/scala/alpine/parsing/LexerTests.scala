import alpine.SourceFile
import alpine.parsing.{Lexer, Token}

class LexerTests extends munit.FunSuite:

  import Token.Kind as K

  test("integer (0pt)") {
    val input = SourceFile("test", "0 001 42 00")
    assert(outputMatches(
      Lexer(input),
      List(K.Integer, K.Integer, K.Integer, K.Integer)))
  }

  test("float (0pt)") {
    val input = SourceFile("test", "0.0 001.00 1. 1.a")
    assert(outputMatches(
      Lexer(input),
      List(K.Float, K.Float, K.Float, K.Float, K.Identifier)))
  }

  test("string (0pt)") {
    val input = SourceFile("test", "\"a c\" \"abc")
    assert(outputMatches(
      Lexer(input),
      List(K.String, K.UnterminatedString)))
  }

  test("label (0pt)") {
    val input = SourceFile("test", "#a #1 #あ #+")
    assert(outputMatches(
      Lexer(input),
      List(K.Label, K.Label, K.Label, K.Undefined, K.Operator)))
  }

  test("identifier (0pt)") {
    val input = SourceFile("test", "_bc a2c ä_ç あ _")
    assert(outputMatches(
      Lexer(input),
      List(K.Identifier, K.Identifier, K.Identifier, K.Identifier, K.Underscore)))
  }

  test("keyword (0pt)") {
    val input = SourceFile("test", "else case fun if let match false then true type")
    assert(outputMatches(
      Lexer(input),
      List(K.Else, K.Case, K.Fun, K.If, K.Let, K.Match, K.False, K.Then, K.True, K.Type)))
  }

  test("special operator (0pt)") {
    val input = SourceFile("test", "-> = @ @! @?")
    assert(outputMatches(
      Lexer(input),
      List(K.Arrow, K.Eq, K.At, K.AtBang, K.AtQuery)))
  }

  test("operator (0pt)") {
    val input = SourceFile("test", "~ ! || && != + - | ^ * / % &")
    assert(outputMatches(
      Lexer(input),
      List.fill(13)(K.Operator)))
  }

  test("punctuation (0pt)") {
    val input = SourceFile("test", ".,:<>(){}")
    assert(outputMatches(
      Lexer(input),
      List(K.Dot, K.Comma, K.Colon, K.LAngle, K.RAngle, K.LParen, K.RParen, K.LBrace, K.RBrace)))
  }

  /** Returns `true` iff the kinds of the tokens produced by `lhs` are the same as `rhs`. */
  private final def outputMatches(
      lhs: Lexer, rhs: List[Token.Kind]
  ): Boolean =
    lhs.next() match
      case Some(t) => rhs match
        case k :: ks =>
          (t.kind == k) && outputMatches(lhs, ks)
        case _ => false
      case _ => rhs.isEmpty

end LexerTests
