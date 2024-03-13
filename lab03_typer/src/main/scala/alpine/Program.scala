package alpine

import alpine.ast

/** The abstract syntax of an Alpine program.
 *
 *  @param declarations The top-level declarations of the program.
 */
final class Program(val declarations: IArray[ast.Declaration]):

  /** A source-level textual representation of this program. */
  def unparsed: String =
    declarations.map((d) => d.unparsed).mkString("\n")

end Program
