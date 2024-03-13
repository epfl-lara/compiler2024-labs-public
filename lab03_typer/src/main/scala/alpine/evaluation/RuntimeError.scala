package alpine
package evaluation

import alpine.Diagnostic

/** An error that occurred during run-time evaluation. */
final class RuntimeError(
    val summary: String, site: SourceSpan
) extends Diagnostic(Diagnostic.Level.Error, site)
