package alpine

/** A single unicode code point. */
type CodePoint = Int

/** Returns `true` iff `self` is a newline. */
def isNewline(self: CodePoint): Boolean =
  (self == '\n') || (self == '\r')

/** Returns `true` iff `self` is a letter or a digit. */
def isAlphaNumeric(self: CodePoint): Boolean =
  Character.isLetter(self) || Character.isDigit(self)

/** Returns `true` if `self` is an operator head. */
def isOperatorHead(self: CodePoint): Boolean =
  "+-*/%&|!?^~".contains(self)

/** Returns `true` if `self` may be part of an operator. */
def isOperatorBody(self: CodePoint): Boolean =
  "<>=+-*/%&|!?^~".contains(self)
