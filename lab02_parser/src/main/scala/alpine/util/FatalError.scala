package alpine
package util

/** An unrecoverable parsing error. */
final class FatalError(message: String, site: SourceSpan) extends Exception
