package alpine.ast

/** An operator identifier. */
enum OperatorIdentifier(val precedence: Int):

  case Tilde extends OperatorIdentifier(OperatorPrecedence.DEFAULT)
  case Bang extends OperatorIdentifier(OperatorPrecedence.DEFAULT)
  case LogicalOr extends OperatorIdentifier(OperatorPrecedence.LOGICAL_DISJUNCTION)
  case LogicalAnd extends OperatorIdentifier(OperatorPrecedence.LOGICAL_CONJUNCTION)
  case LessThan extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case LessThanOrEqual extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case GreaterThan extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case GreaterThanOrEqual extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case Equal extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case NotEqual extends OperatorIdentifier(OperatorPrecedence.COMPARISON)
  case ClosedRange extends OperatorIdentifier(OperatorPrecedence.RANGE_FORMATION)
  case HaflOpenRange extends OperatorIdentifier(OperatorPrecedence.RANGE_FORMATION)
  case Plus extends OperatorIdentifier(OperatorPrecedence.ADDITION)
  case Minus extends OperatorIdentifier(OperatorPrecedence.ADDITION)
  case BitwiseOr extends OperatorIdentifier(OperatorPrecedence.ADDITION)
  case BitwiseXor extends OperatorIdentifier(OperatorPrecedence.ADDITION)
  case Star extends OperatorIdentifier(OperatorPrecedence.MULTIPLICATION)
  case Slash extends OperatorIdentifier(OperatorPrecedence.MULTIPLICATION)
  case Percent extends OperatorIdentifier(OperatorPrecedence.MULTIPLICATION)
  case Ampersand extends OperatorIdentifier(OperatorPrecedence.MULTIPLICATION)
  case LeftShift extends OperatorIdentifier(OperatorPrecedence.BITWISE_SHIFT)
  case RightShift extends OperatorIdentifier(OperatorPrecedence.BITWISE_SHIFT)

  override def toString: String =
    this match
      case Tilde => "~"
      case Bang => "!"
      case LogicalOr => "||"
      case LogicalAnd => "&&"
      case LessThan => "<"
      case LessThanOrEqual => "<="
      case GreaterThan => ">"
      case GreaterThanOrEqual => ">="
      case Equal => "=="
      case NotEqual => "!="
      case ClosedRange => "..."
      case HaflOpenRange => "..<"
      case Plus => "+"
      case Minus => "-"
      case BitwiseOr => "|"
      case BitwiseXor => "^"
      case Star => "*"
      case Slash => "/"
      case Percent => "%"
      case Ampersand => "&"
      case LeftShift => "<<"
      case RightShift => ">>"

end OperatorIdentifier
