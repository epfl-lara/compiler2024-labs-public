package alpine.parsing

import alpine.{Diagnostic, SourceSpan}

/** An error that occurred during parsing. */
class SyntaxError(
    val summary: String, site: SourceSpan
) extends Diagnostic(Diagnostic.Level.Error, site)

/** An error that occurred because a parser failed to consume a specific token. */
final class ExpectedTokenError(
    kind: Token.Kind, site: SourceSpan
) extends SyntaxError(s"expected ${kind}", site)

/** An error that occurred because a parser failed to parse a tree of the given kind` */
final class ExpectedTree(
    kind: String, site: SourceSpan
) extends SyntaxError(s"expected ${kind}", site)
