package alpine
package parsing

/** A terminal symbol of the syntactic grammar. */
final case class Token(val kind: Token.Kind, val site: SourceSpan) extends SourceRepresentable:

  /** Returns `true` iff `this` is spelled with `s` and can be part of an operator. */
  def isOperatorPart(s: String): Boolean =
    kind.isOperatorPart && (site.text == s)

  /** Returns `true` iff `this` starts at `end` and can be part of an operator. */
  def isOperatorPartImmediatelyAfter(end: Int): Boolean =
    kind.isOperatorPart && (end == site.start)

  /** A textual representation of this token usable for debugging. */
  override def toString: String =
    s"${kind.toString}(${site.start}:${site.end})"

end Token

object Token:

  /** The kind of a token. */
  enum Kind:

    case Underscore
    case Identifier
    case True
    case False
    case Let
    case Fun
    case Type
    case If
    case Then
    case Else
    case Match
    case Case

    case Integer
    case Float
    case String
    case Label

    case Operator
    case Arrow
    case Eq
    case At
    case AtBang
    case AtQuery

    case Dot
    case Comma
    case Colon
    case LAngle
    case RAngle
    case LParen
    case RParen
    case LBrace
    case RBrace

    case Undefined
    case UnterminatedString

    /** Returns a token of this kind at `site`. */
    def apply(site: SourceSpan): Token =
      Token(this, site)

    /** Returns `true` iff the kind of `scrutinee` is `this`. */
    def matches(scrutinee: Token): Boolean =
      scrutinee.kind == this

    /** Returns `true` iff `this` denotes a keyword. */
    def isKeyword: Boolean =
      this match
        case True | False | Let | Fun | Type | If | Then | Else | Match | Case => true
        case _ => false

    /** Returns `true` iff the `this` denotes tokens that may be part of an operator. */
    def isOperatorPart: Boolean =
      this match
        case Operator | Eq | LAngle | RAngle | Dot => true
        case _ => false

  end Kind

end Token
