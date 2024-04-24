package alpine.ast

object OperatorPrecedence:

  inline val DEFAULT = 0
  inline val LOGICAL_DISJUNCTION = 1
  inline val LOGICAL_CONJUNCTION = 2
  inline val COMPARISON = 3
  inline val RANGE_FORMATION = 4
  inline val ADDITION = 5
  inline val MULTIPLICATION = 6
  inline val BITWISE_SHIFT = 7

  /** Returns the minimum precedence. */
  inline def min: Int = DEFAULT

  /** Returns the maximum precedence. */
  inline def max: Int = BITWISE_SHIFT

end OperatorPrecedence