package alpine
package parsing

import alpine.util.Substring

import scala.annotation.tailrec

/** A lexer splitting character strings into tokens.
 *
 * @param source The character strings from which tokens are produced.
 */
class Lexer(val source: SourceFile):

  /** The index of the next character to read from `source`. */
  private var index: CodePoint = 0

  /** Returns the remaining of the tokens produced by this instance. */
  def toList: List[Token] =
    @tailrec def loop(s: Lexer, r: List[Token]): List[Token] =
      s.next() match
        case Some(t) => loop(s, r :+ t)
        case _ => r
    loop(this, List())

  /** Returns a copy of this instance. */
  def copy(): Lexer =
    val clone = Lexer(source)
    clone.index = index
    clone

  /** Returns the next token, if any. */
  def next(): Option[Token] =
    if !skipWhitespaceAndComments() then None else peek.map {
      case '#' =>
        label
      case '@' =>
        typecast
      case '\"' =>
        stringLiteral
      case h if Character.isDigit(h) =>
        numberLiteral
      case h if Character.isLetter(h) || (h == '_') =>
        identifierOrKeyword
      case h if isOperatorHead(h) =>
        operator
      case _ =>
        punctuationOrUndefied
    }

  /** Advances `index` to the first character that isn't a whitespace and isn't in a comment. */
  @tailrec private def skipWhitespaceAndComments(): Boolean =
    if index >= source.length then
      false
    else if Character.isWhitespace(source(index)) then
      take()
      skipWhitespaceAndComments()
    else if !takeString("//").isEmpty then
      takeWhile((c) => !isNewline(c))
      skipWhitespaceAndComments()
    else
      true

  /** Consumes and returns a label. */
  private def label: Token =
    val start = index
    take()
    val p = takeWhile((c) => isAlphaNumeric(c) || (c == '_'))
    if p.isEmpty then
      Token.Kind.Undefined(source.span(start, p.end))
    else
      Token.Kind.Label(source.span(start, p.end))

  /** Consumes and returns a string literal. */
  private def stringLiteral: Token =
    @tailrec def loop(p: Int): Token =
      if p == source.length then
        makeToken(Token.Kind.UnterminatedString, p)
      else if source(p) == '\"' then
        makeToken(Token.Kind.String, p + 1)
      else
        loop(p + 1)
    loop(index + 1)

  /** Consumes and returns a number literal. */
  private def numberLiteral: Token =
    val integer = takeWhile((c) => Character.isDigit(c))
    if peek == Some('.') then
      take()
      val fraction = takeWhile((c) => Character.isDigit(c))
      Token.Kind.Float(source.span(integer.start, fraction.end))
    else
      Token.Kind.Integer(integer)

  /** Consumes and returns an identifier or keyword. */
  private def identifierOrKeyword: Token =
    val p = takeWhile((c) => isAlphaNumeric(c) || (c == '_'))
    val s = Substring(source.text, p.start, p.end)

    if s == "_" then
      Token.Kind.Underscore(p)
    else if s == "true" then
      Token.Kind.True(p)
    else if s == "false" then
      Token.Kind.False(p)
    else if s == "let" then
      Token.Kind.Let(p)
    else if s == "fun" then
      Token.Kind.Fun(p)
    else if s == "type" then
      Token.Kind.Type(p)
    else if s == "if" then
      Token.Kind.If(p)
    else if s == "then" then
      Token.Kind.Then(p)
    else if s == "else" then
      Token.Kind.Else(p)
    else if s == "match" then
      Token.Kind.Match(p)
    else if s == "case" then
      Token.Kind.Case(p)
    else
      Token.Kind.Identifier(p)

  /** Consumes and returns a typecast operator. */
  private def typecast: Token =
    val start = index
    take()
    val result = peek.getOrElse(-1) match
      case '!' => Token.Kind.AtBang(source.span(start, start + 2))
      case '?' => Token.Kind.AtQuery(source.span(start , start + 2))
      case _ => Token.Kind.At(source.span(start, start + 1))
    index = result.site.end
    result

  /** Consumes and returns an operator. */
  private def operator: Token =
    val p = takeWhile((c) => isOperatorBody(c))
    val s = Substring(source.text, p.start, p.end)

    if s == "->" then
      Token.Kind.Arrow(p)
    else
      Token.Kind.Operator(p)

  /** Consumes and returns a punctuation symbol, or an undefined token if the stream doesn't start
   *  with punctuation.
   */
  private def punctuationOrUndefied: Token =
    val start = index
    def p = source.span(start, index)

    take().get match
      case '.' => Token.Kind.Dot(p)
      case ',' => Token.Kind.Comma(p)
      case ':' => Token.Kind.Colon(p)
      case '=' => Token.Kind.Eq(p)
      case '<' => Token.Kind.LAngle(p)
      case '>' => Token.Kind.RAngle(p)
      case '(' => Token.Kind.LParen(p)
      case ')' => Token.Kind.RParen(p)
      case '{' => Token.Kind.LBrace(p)
      case '}' => Token.Kind.RBrace(p)
      case _ => Token.Kind.Undefined(p)

  /** Returns an undefined token consuming the remainder of the stream. */
  private def undefined: Token =
    makeToken(Token.Kind.Undefined, source.length)

  /** Returns the next character in the stream without consuming it. */
  private def peek: Option[Int] =
    if index < source.length then Some(source(index)) else None

  /** Consumes and returns the next character in the stream. */
  private def take(): Option[Int] =
    if index < source.length then
      val c = source(index)
      index += 1
      Some(c)
    else
      None

  /** Consumes `other` from the stream and returns its positions in `source` if the stream starts
   *  with that string; otherwise, or returns an empty range.
   */
  private def takeString(other: String): SourceSpan =
    if startsWith(other) then
      val start = index
      index += other.length
      source.span(start, index)
    else
      source.span(index, index)

  /** Consumes the longest sequence of characters satisfying `predicate` and returns its positions
   *  in `source`.
   */
  private def takeWhile(predicate: Int => Boolean): SourceSpan =
    @tailrec def loop(start: Int): SourceSpan =
      if (index < source.length) && predicate(source(index)) then
        index += 1
        loop(start)
      else
        source.span(start, index)
    loop(index)

  /** Returns `true` iff the next elements in the stream are equal to `other`. */
  private def startsWith(other: String): Boolean =
    @tailrec def loop(p: Int, q: Int): Boolean =
      (q >= other.length)
        || ((p < source.length) && (source(p) == other(q)) && loop(p + 1, q + 1))
    loop(index, 0)

  /** Moves the head of the stream to `end` and returns a token of kind `k` over [index, end). */
  private def makeToken(k: Token.Kind, end: Int): Token =
    val s = index
    index = end
    k(source.span(s, end))

end Lexer
