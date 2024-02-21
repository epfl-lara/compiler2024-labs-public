package alpine
package ast

import alpine.parsing.Token
import alpine.symbols

/** An abstract syntax tree. */
trait Tree extends SourceRepresentable:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B

  /** Traverses `this` in pre-order, notifying `w` when entering or exiting a node. */
  def walk[W <: TreeWalker](w: W): Unit

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit

  /** The type of the tree. */
  final def tpe(using p: TypedProgram): symbols.Type =
    p.treeToType(this)

  /** The entity referred to by the tree, if any. */
  def referredEntity(using p: TypedProgram): Option[symbols.EntityReference] =
    p.treeToReferredEntity.get(this)

  /** A source-level textual representation of this tree.
   *
   *  Abstract syntax trees drop some information about the concrete source-level representation.
   *  Hence, "unparsing" a tree isn't guaranteed to produce an output identical to the text from
   *  which it was parsed.
   *
   *  You can use `site.text` to get the exact text from which the tree was parsed if it hasn't
   *  been synthesized.
   */
  def unparsed: String =
    toString

end Tree

object Tree:

  /** Traverses `roots` in pre-order, notifying `w` when entering or exiting a node. */
  def walkRoots[W <: TreeWalker](roots: Iterable[Tree], w: W): Unit =
    for n <- roots do n.walk(w)

  /** Traverses `n` in pre-order, notifying `w` when entering or exiting a node. */
  def walkOption[W <: TreeWalker](n: Option[Tree], w: W): Unit =
    n.map(_.walk(w))

end Tree

/** The base class of parenthesized trees. */
abstract class ParenthesizedPrototype[T <: Tree](inner: T, site: SourceSpan) extends Tree:

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  final def walkChildren[W <: TreeWalker](w: W): Unit =
    inner.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"(${inner.unparsed})"

end ParenthesizedPrototype

/** The base class of record trees.
 *
 *  Record expressions, types, and patterns share the same structure, which this class implements.
 */
abstract class RecordPrototype[F <: Labeled[Tree]](
    identifier: String,
    fields: List[F],
    site: SourceSpan
) extends Tree:

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  final def walkChildren[W <: TreeWalker](w: W): Unit =
    for f <- fields do f.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    if fields.isEmpty then identifier else
      val a = fields.map((a) => a.unparsed).mkString(", ")
      s"${identifier}(${a})"

end RecordPrototype

/** A tree with an optional label. */
final case class Labeled[+T <: Tree](
    label: Option[String],
    value: T,
    site: SourceSpan
) extends Tree:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitLabeled(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterLabeled(this) then
      walkChildren(w)
      w.exitLabeled(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    value.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    label match
      case Some(s) => s"${s}: ${value.unparsed}"
      case _ => value.unparsed

end Labeled

/** An abstract syntax representing source code skipped to recover from a parse error. */
final case class ErrorTree(site: SourceSpan)
extends Declaration with Expression with Type with Pattern:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitError(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterError(this) then
      walkChildren(w)
      w.exitError(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    ()

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    "<#error#>"

end ErrorTree

// --- Declarations -----------------------------------------------------------

/** An abstract syntax tree representing the declaration of an entity. */
sealed trait Declaration extends Tree:

  /** The entity introduced by this declaration. */
  final def entityDeclared(using p: TypedProgram): symbols.Entity =
    symbols.Entity.Declaration(nameDeclared, tpe)

  /** The name introduced by this declaration. */
  final def nameDeclared(using p: TypedProgram): symbols.Name =
    p.declarationToNameDeclared(this)

end Declaration

/** A binding declaration. */
final case class Binding(
    identifier: String,
    ascription: Option[Type],
    initializer: Option[Expression],
    site: SourceSpan
) extends Declaration with Pattern:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitBinding(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterBinding(this) then
      walkChildren(w)
      w.exitBinding(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkOption(ascription, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val i = initializer.map((e) => s" = ${e.unparsed}").getOrElse("")
    ascription match
      case Some(a) => s"let ${identifier}: ${a.unparsed}${i}"
      case _ => s"let ${identifier}${i}"

end Binding

/** A type declaration. */
final case class TypeDeclaration(
    identifier: String,
    genericParameters: List[Parameter],
    body: Type,
    site: SourceSpan
) extends Declaration:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitTypeDeclaration(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterTypeDeclaration(this) then
      walkChildren(w)
      w.exitTypeDeclaration(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkRoots(genericParameters, w)
    body.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val g = if genericParameters.isEmpty then "" else
      "<" + genericParameters.map((p) => p.unparsed).mkString(", ") + ">"
    s"type ${identifier}${g} = ${body.unparsed}"

end TypeDeclaration

/** A function declaration. */
final case class Function(
    identifier: String,
    genericParameters: List[Parameter],
    inputs: List[Parameter],
    output: Option[Type],
    body: Expression,
    site: SourceSpan
) extends Declaration:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitFunction(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterFunction(this) then
      walkChildren(w)
      w.exitFunction(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkRoots(genericParameters, w)
    Tree.walkRoots(inputs, w)
    Tree.walkOption(output, w)
    body.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val g = if genericParameters.isEmpty then "" else
      "<" + genericParameters.map((p) => p.unparsed).mkString(", ") + ">"
    val i = inputs.map((p) => p.unparsed).mkString(", ")
    val s = output match
      case Some(o) => s"${g}(${i}) -> ${o.unparsed}"
      case _ => s"${g}(${i})"
    s"fun ${identifier}${s} { ${body.unparsed} }"

end Function

/** The declaration of a parameter of a function, type, or lambda. */
final case class Parameter(
    label: Option[String],
    identifier: String,
    ascription: Option[Type],
    site: SourceSpan
) extends Declaration:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitParameter(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterParameter(this) then
      walkChildren(w)
      w.exitParameter(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkOption(ascription, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val api = label
      .map((l) => if l == identifier then identifier else s"${l} ${identifier}")
      .getOrElse(s"_ ${identifier}")
    ascription match
      case Some(a) => s"${api}: ${a.unparsed}"
      case _ => s"${api}"

end Parameter

// --- Expressions (i.e., term-level expressions) -----------------------------

/** An abstract syntax tree representing a term-level expression. */
sealed trait Expression extends Tree

/** The identification of a part selected by a `Select` expression. */
sealed trait Selectee extends Expression

/** A value identifier. */
final case class Identifier(value: String, site: SourceSpan) extends Selectee:

  /** Creates an instance from the given token. */
  def this(t: Token) =
    this(t.site.text.toString, t.site)

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitIdentifier(this)

  /** Traverses `this` in pre-order, notifying `w` when entering or exiting a node. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterIdentifier(this) then
      walkChildren(w)
      w.exitIdentifier(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    ()

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    value

end Identifier

/** A literal expression. */
abstract class Literal(value: String, site: SourceSpan) extends Expression:

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  final def walkChildren[W <: TreeWalker](w: W): Unit =
    ()

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    value

end Literal

/** A Boolean literal expression. */
final case class BooleanLiteral(value: String, site: SourceSpan)
extends Literal(value, site):

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitBooleanLiteral(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterBooleanLiteral(this) then
      walkChildren(w)
      w.exitBooleanLiteral(this)

end BooleanLiteral

/** An integer literal expression. */
final case class IntegerLiteral(value: String, site: SourceSpan)
extends Literal(value, site) with Selectee:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitIntegerLiteral(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterIntegerLiteral(this) then
      walkChildren(w)
      w.exitIntegerLiteral(this)

end IntegerLiteral

/** A floating-point literal expression. */
final case class FloatLiteral(value: String, site: SourceSpan)
extends Literal(value, site):

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitFloatLiteral(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterFloatLiteral(this) then
      walkChildren(w)
      w.exitFloatLiteral(this)

end FloatLiteral

/** A string literal expression. */
final case class StringLiteral(value: String, site: SourceSpan)
extends Literal(value, site):

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitStringLiteral(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterStringLiteral(this) then
      walkChildren(w)
      w.exitStringLiteral(this)

end StringLiteral

/** A record expression. */
final case class Record(
    identifier: String,
    fields: List[Labeled[Expression]],
    site: SourceSpan
) extends RecordPrototype(identifier, fields, site) with Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitRecord(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterRecord(this) then
      walkChildren(w)
      w.exitRecord(this)

end Record

/** A qualified identifier. */
final case class Selection(
    qualification: Expression,
    selectee: Selectee,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitSelection(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterSelection(this) then
      walkChildren(w)
      w.exitSelection(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    selectee.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"${qualification.unparsed}.${selectee.unparsed}"

end Selection

/** A term-level application. */
final case class Application(
    function: Expression,
    arguments: List[Labeled[Expression]],
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitApplication(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterApplication(this) then
      walkChildren(w)
      w.exitApplication(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    function.walk(w)
    Tree.walkRoots(arguments, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val a = arguments.map((a) => a.unparsed).mkString(", ")
    s"${function.unparsed}(${a})"

end Application

/** A term-level application using prefix notation. */
final case class PrefixApplication(
    function: Identifier,
    argument: Expression,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitPrefixApplication(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterPrefixApplication(this) then
      walkChildren(w)
      w.exitPrefixApplication(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    function.walk(w)
    argument.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"(${function.unparsed}${argument.unparsed})"

end PrefixApplication

/** A term-level application using an infix notation. */
final case class InfixApplication(
    function: Identifier,
    lhs: Expression,
    rhs: Expression,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitInfixApplication(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterInfixApplication(this) then
      walkChildren(w)
      w.exitInfixApplication(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    function.walk(w)
    lhs.walk(w)
    rhs.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"(${lhs.unparsed} ${function.unparsed} ${rhs.unparsed})"

end InfixApplication

/** A conditional expression (aka "if"). */
final case class Conditional(
    condition: Expression,
    successCase: Expression,
    failureCase: Expression,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitConditional(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterConditional(this) then
      walkChildren(w)
      w.exitConditional(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    condition.walk(w)
    successCase.walk(w)
    failureCase.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"if ${condition.unparsed} then ${successCase.unparsed} else ${failureCase.unparsed}"

end Conditional

/** A match expression. */
final case class Match(
  scrutinee: Expression,
  cases: List[Match.Case],
  site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitMatch(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterMatch(this) then
      walkChildren(w)
      w.exitMatch(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    scrutinee.walk(w)
    Tree.walkRoots(cases, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val c = cases.map((c) => c.unparsed).mkString(" ")
    s"match ${scrutinee.unparsed} { ${c} }"

end Match

object Match:

  /** A case in a match expression. */
  final case class Case(
      pattern: Pattern,
      body: Expression,
      site: SourceSpan
  ) extends Tree:

    /** Returns the result of calling the visitor method of `v` for `this`. */
    def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
      v.visitMatchCase(this)

    /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
    def walk[W <: TreeWalker](w: W): Unit =
      if w.enterMatchCase(this) then
        walkChildren(w)
        w.exitMatchCase(this)

    /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
    def walkChildren[W <: TreeWalker](w: W): Unit =
      pattern.walk(w)
      body.walk(w)

    /** A source-level textual representation of this tree. */
    override def unparsed: String =
      s"case ${pattern.unparsed} then ${body.unparsed}"

  end Case

end Match

/** A let expression. */
final case class Let(
    binding: Binding,
    body: Expression,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitLet(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterLet(this) then
      walkChildren(w)
      w.exitLet(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    binding.walk(w)
    body.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"${binding.unparsed} { ${body.unparsed} }"

end Let

/** A lambda expression (aka an anonymous function). */
final case class Lambda(
    inputs: List[Parameter],
    output: Option[Type],
    body: Expression,
    site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitLambda(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterLambda(this) then
      walkChildren(w)
      w.exitLambda(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkRoots(inputs, w)
    Tree.walkOption(output, w)
    body.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val i = inputs.map((p) => p.unparsed).mkString(", ")
    output match
      case Some(o) => s"(${i}) -> ${o.unparsed} { ${body.unparsed} }"
      case _ => s"(${i}) { ${body} }"

end Lambda

/** A term-level expression in parentheses. */
final case class ParenthesizedExpression(
    inner: Expression, site: SourceSpan
) extends ParenthesizedPrototype(inner, site) with Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitParenthesizedExpression(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterParenthesizedExpression(this) then
      walkChildren(w)
      w.exitParenthesizedExpression(this)

end ParenthesizedExpression

/** A term with an ascription. */
final case class AscribedExpression(
    inner: Expression, operation: Typecast, ascription: Type, site: SourceSpan
) extends Expression:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitAscribedExpression(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterAscribedExpression(this) then
      walkChildren(w)
      w.exitAscribedExpression(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    inner.walk(w)
    ascription.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    s"${inner.unparsed} @ ${ascription.unparsed}"

end AscribedExpression

// --- Types (i.e., type-level expressions) ---------------------------------

/** An abstract syntax tree representing a type-level expression. */
sealed trait Type extends Tree

/** A value identifier. */
final case class TypeIdentifier(value: String, site: SourceSpan) extends Type:

  /** Creates an instance from the given token. */
  def this(t: Token) =
    this(t.site.text.toString, t.site)

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitTypeIdentifier(this)

  /** Traverses `this` in pre-order, notifying `w` when entering or exiting a node. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterTypeIdentifier(this) then
      walkChildren(w)
      w.exitTypeIdentifier(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    ()

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    value

end TypeIdentifier

/** A record expression. */
final case class RecordType(
    identifier: String,
    fields: List[Labeled[Type]],
    site: SourceSpan
) extends RecordPrototype(identifier, fields, site) with Type:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitRecordType(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterRecordType(this) then
      walkChildren(w)
      w.exitRecordType(this)

end RecordType

/** A type-level application. */
final case class TypeApplication(
    constructor: TypeIdentifier,
    arguments: List[Labeled[Type]],
    site: SourceSpan
) extends Type:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitTypeApplication(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterTypeApplication(this) then
      walkChildren(w)
      w.exitTypeApplication(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    constructor.walk(w)
    Tree.walkRoots(arguments, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val a = arguments.map((a) => a.unparsed).mkString(", ")
    s"${constructor.unparsed}<${a}>"

end TypeApplication

/** A lambda expression (aka an anonymous function). */
final case class Arrow(
    inputs: List[Labeled[Type]],
    output: Type,
    site: SourceSpan
) extends Type:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitArrow(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterArrow(this) then
      walkChildren(w)
      w.exitArrow(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkRoots(inputs, w)
    output.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    val i = inputs.map((p) => p.unparsed).mkString(", ")
    s"(${i}) -> ${output.unparsed}"

end Arrow

/** A type sum expression. */
final case class Sum(members: List[Type], site: SourceSpan) extends Type:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitSum(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterSum(this) then
      walkChildren(w)
      w.exitSum(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    Tree.walkRoots(members, w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    members.map((p) => p.unparsed).mkString(" | ")

end Sum

/** A type-level expression in parentheses. */
final case class ParenthesizedType(
    inner: Type, site: SourceSpan
) extends ParenthesizedPrototype(inner, site) with Type:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitParenthesizedType(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterParenthesizedType(this) then
      walkChildren(w)
      w.exitParenthesizedType(this)

end ParenthesizedType

// --- Patterns -------------------------------------------------------------

/** An abstract syntax tree representing a pattern. */
sealed trait Pattern extends Tree:

  /** A sequence with the declarations in this pattern together with their path. */
  final def declarationsWithPath: List[(Binding, IArray[Int])] =
    /** The declarations in `self` with their path relative to `reversedPrefix`. */
    def gather(self: Pattern, reversedPrefix: List[Int]): List[(Binding, IArray[Int])] =
      self match
        case p: Binding =>
          List((p, IArray.from(reversedPrefix.reverseIterator)))
        case p: RecordPattern =>
          p.fields.zipWithIndex
            .map((f, i) => gather(f.value, reversedPrefix.prepended(i)))
            .flatten
        case _ => List()
    gather(this, List())

end Pattern

/** A pattern that matches the value of an expression. */
final case class ValuePattern(value: Expression, site: SourceSpan) extends Pattern:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitValuePattern(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterValuePattern(this) then
      walkChildren(w)
      w.exitValuePattern(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    value.walk(w)

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    value.unparsed

end ValuePattern

/** A pattern that matches record values. */
final case class RecordPattern(
    identifier: String,
    fields: List[Labeled[Pattern]],
    site: SourceSpan
) extends RecordPrototype(identifier, fields, site) with Pattern:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitRecordPattern(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterRecordPattern(this) then
      walkChildren(w)
      w.exitRecordPattern(this)

end RecordPattern

/** A pattern that matches any value. */
final case class Wildcard(site: SourceSpan) extends Pattern:

  /** Returns the result of calling the visitor method of `v` for `this`. */
  def visit[A, B](v: TreeVisitor[A, B])(using a: A): B =
    v.visitWildcard(this)

  /** Visits `this` and its children in pre-order, notifying `w` when a node is entered or left. */
  def walk[W <: TreeWalker](w: W): Unit =
    if w.enterWildcard(this) then
      walkChildren(w)
      w.exitWildcard(this)

  /** Traverses `this`'s children' in pre-order, notifying `w` when entering or exiting a node. */
  def walkChildren[W <: TreeWalker](w: W): Unit =
    ()

  /** A source-level textual representation of this tree. */
  override def unparsed: String =
    "_"

end Wildcard
