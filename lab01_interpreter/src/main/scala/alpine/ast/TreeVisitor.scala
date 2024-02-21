package alpine.ast

import alpine.util.FatalError

/** A type implementing callbacks for performing an operation on the nodes of an AST.
 *
 *  @tparam A The type of the operation's local state.
 *  @tparam B The result the operation.
 */
trait TreeVisitor[A, B]:

  /** Visits `n` with state `a`. */
  def visitLabeled[T <: Tree](n: Labeled[T])(using a: A): B

  /** Visits `n` with state `a`. */
  def visitBinding(n: Binding)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitTypeDeclaration(n: TypeDeclaration)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitFunction(n: Function)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitParameter(n: Parameter)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitIdentifier(n: Identifier)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitBooleanLiteral(n: BooleanLiteral)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitIntegerLiteral(n: IntegerLiteral)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitFloatLiteral(n: FloatLiteral)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitStringLiteral(n: StringLiteral)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitRecord(n: Record)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitSelection(n: Selection)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitApplication(n: Application)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitPrefixApplication(n: PrefixApplication)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitInfixApplication(n: InfixApplication)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitConditional(n: Conditional)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitMatch(n: Match)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitMatchCase(n: Match.Case)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitLet(n: Let)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitLambda(n: Lambda)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitParenthesizedExpression(n: ParenthesizedExpression)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitAscribedExpression(n: AscribedExpression)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitTypeIdentifier(n: TypeIdentifier)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitRecordType(n: RecordType)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitTypeApplication(n: TypeApplication)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitArrow(n: Arrow)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitSum(n: Sum)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitParenthesizedType(n: ParenthesizedType)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitValuePattern(n: ValuePattern)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitRecordPattern(n: RecordPattern)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitWildcard(n: Wildcard)(using a: A): B

  /** Visits `n` with state `a`. */
  def visitError(n: ErrorTree)(using a: A): B

  /** Throws an error indicating that visiting `n` wasn't expected. */
  final def unexpectedVisit(n: Tree): Nothing =
    throw FatalError(s"unexpected visit of '${n.getClass.getName}'", n.site)

end TreeVisitor
