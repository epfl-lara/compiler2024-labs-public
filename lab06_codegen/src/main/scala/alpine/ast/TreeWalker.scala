package alpine.ast

/** A type that is notified when nodes are entered and left during a tree traversal.
 *
 *  Override `enterT` to perform actions before a node `n` of type `T` is being traversed and/or
 *  customize how the tree is traversed. If the method returns `true`, `enterU` will be called
 *  before each child of `n` is entered and `exitT` will be called when `n` is left. If it returns
 *  `false`, none of `n`'s children will be visited and `exitT` won't be called.
 */
trait TreeWalker:

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterAny(n: Tree): Boolean =
    true

  /** Called when `n` is about to be left. */
  def exitAny(n: Tree): Unit =
    ()

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterLabeled[T <: Tree](n: Labeled[T]): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitLabeled[T <: Tree](n: Labeled[T]): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterBinding(n: Binding): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitBinding(n: Binding): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterTypeDeclaration(n: TypeDeclaration): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitTypeDeclaration(n: TypeDeclaration): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterFunction(n: Function): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitFunction(n: Function): Unit =
    exitAny(n)

    /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterParameter(n: Parameter): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitParameter(n: Parameter): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterIdentifier(n: Identifier): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitIdentifier(n: Identifier): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterBooleanLiteral(n: BooleanLiteral): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitBooleanLiteral(n: BooleanLiteral): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterIntegerLiteral(n: IntegerLiteral): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitIntegerLiteral(n: IntegerLiteral): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterFloatLiteral(n: FloatLiteral): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitFloatLiteral(n: FloatLiteral): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterStringLiteral(n: StringLiteral): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitStringLiteral(n: StringLiteral): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterRecord(n: Record): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitRecord(n: Record): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterSelection(n: Selection): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitSelection(n: Selection): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterApplication(n: Application): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitApplication(n: Application): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterPrefixApplication(n: PrefixApplication): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitPrefixApplication(n: PrefixApplication): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterInfixApplication(n: InfixApplication): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitInfixApplication(n: InfixApplication): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterConditional(n: Conditional): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitConditional(n: Conditional): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterMatch(n: Match): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitMatch(n: Match): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterMatchCase(n: Match.Case): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitMatchCase(n: Match.Case): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterLet(n: Let): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitLet(n: Let): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterLambda(n: Lambda): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitLambda(n: Lambda): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterParenthesizedExpression(n: ParenthesizedExpression): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitParenthesizedExpression(n: ParenthesizedExpression): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterAscribedExpression(n: AscribedExpression): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitAscribedExpression(n: AscribedExpression): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterTypeIdentifier(n: TypeIdentifier): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitTypeIdentifier(n: TypeIdentifier): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterRecordType(n: RecordType): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitRecordType(n: RecordType): Unit =
    exitAny(n)

    /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterTypeApplication(n: TypeApplication): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitTypeApplication(n: TypeApplication): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterArrow(n: Arrow): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitArrow(n: Arrow): Unit =
    exitAny(n)

    /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterSum(n: Sum): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitSum(n: Sum): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterParenthesizedType(n: ParenthesizedType): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitParenthesizedType(n: ParenthesizedType): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterValuePattern(n: ValuePattern): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitValuePattern(n: ValuePattern): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterRecordPattern(n: RecordPattern): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitRecordPattern(n: RecordPattern): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterWildcard(n: Wildcard): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitWildcard(n: Wildcard): Unit =
    exitAny(n)

  /** Called when `n` is about to be entered; returns `false` if traversal should skip `n`. */
  def enterError(n: ErrorTree): Boolean =
    enterAny(n)

  /** Called when `n` is about to be left. */
  def exitError(n: ErrorTree): Unit =
    exitAny(n)

end TreeWalker
