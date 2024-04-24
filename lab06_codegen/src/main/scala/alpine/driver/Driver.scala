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

/** Rewrites the input program in Scala using the given `configuration`. */
def transpile(configuration: Configuration): Unit =
  val typedSyntax = typeCheck(configuration)
  val transpiler = codegen.ScalaPrinter(typedSyntax)
  val output = transpiler.transpile()
  println(output)

/** Rewrites the input program in Scala using the given `configuration`. */
def compile(configuration: Configuration): Unit =
  val typedSyntax = typeCheck(configuration)
  val cg = codegen.CodeGenerator(typedSyntax)
  val module = cg.compile()
  wasm.Wasm.writeToFile("output.wat", module)
  wasm.Wasm.watToWasm("output.wat", "output.wasm")

def run(configuration: Configuration): Unit =
  compile(configuration)
  if configuration.nodeDebug then wasm.Wasm.debugNode("output.wasm") else wasm.Wasm.runNode("output.wasm")

/** Interpret the input program with the given `configuration` and returns its exit status. */
def interpret(configuration: Configuration): Int =
  val typedSyntax = typeCheck(configuration)
  val interpreter = evaluation.Interpreter(
    typedSyntax,
    configuration.standardOutput,
    configuration.standardError)
  interpreter.run()
