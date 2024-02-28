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