package alpine
package driver

import scala.util.{Success, Failure}

/** Run syntax analysis with the given `configuration`. */
def parse(configuration: Configuration): Program =
  val parser = parsing.Parser(configuration.inputs.head) // TODO: multiple inputs
  val syntax = parser.program()
  val ds = parser.diagnostics
  ds.throwOnError()
  ds.log()
  syntax

/** Run semantic analysis with the given `configuration`. */
def typeCheck(configuration: Configuration): TypedProgram =
  val syntax = parse(configuration)
  val typer = typing.Typer(configuration.traceInference)
  val typedSyntax = typer.check(syntax)
  val ds = typer.diagnostics
  ds.throwOnError()
  ds.log()
  typedSyntax