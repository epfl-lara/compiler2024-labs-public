package alpine
package typing

import alpine.Diagnostic

/** An error that occurred during type checking. */
final class TypeError(
    val summary: String, site: SourceSpan
) extends Diagnostic(Diagnostic.Level.Error, site)

/** A warning detected during type checking. */
final class TypeWarning(
    val summary: String, site: SourceSpan
) extends Diagnostic(Diagnostic.Level.Warning, site)
