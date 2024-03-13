package alpine

import alpine.ast
import alpine.symbols.{Type, EntityReference}

/** The abstract syntax of an Alpine program annotated with type information.
 *
 *  @param untyped The unnanotated program.
 *  @param treeToType A map from tree to its type.
 *  @param scopeToName A map from scope to its name.
 *  @param declarationToScope A map from declaration to the innermost scope containing it.
 *  @param declarationToNameDeclared A map from declaration to the name that it introduces.
 *  @param treeToReferredEntity A map from tree to the entity to which it refers.
 */
final class TypedProgram(
    val untyped: Program,
    val treeToType: Map[ast.Tree, Type],
    val scopeToName: Map[ast.Tree, symbols.Name],
    val declarationToScope: Map[ast.Declaration, symbols.Name],
    val declarationToNameDeclared: Map[ast.Declaration, symbols.Name],
    val treeToReferredEntity: Map[ast.Tree, EntityReference]
):

  /** The top-level declarations of the program. */
  def declarations: IArray[ast.Declaration] =
    untyped.declarations

  /** The program's entry point. */
  val entry: Option[ast.Expression] =
    declarations
      .collectFirst({ case d: ast.Binding if d.identifier == "main" => d })
      .map(_.initializer.get)

end TypedProgram
