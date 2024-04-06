import alpine.{DiagnosticSet, SourceFile}
import alpine.driver
import alpine.util.toSubstring

import scala.util.{Success, Failure}

/** The operation to perform on the compiler's input. */
private enum Action:
  case Parse, TypeCheck, Transpile, Compile, Interpret

/** Parses a the action to run and its configuration from the command line arguments `args`.*/
private def parseCommandLineArguments(args: Seq[String]): (Action, driver.Configuration) =
  var action = Action.Compile
  var inputPaths = List[String]()
  var traceInference = false

  for a <- args do
    val s = a.toSubstring
    if s.startsWith("--") then
      if s.drop(2) == "trace-inference" then
        traceInference = true
    else if s.startsWith("-") then
      s.drop(1).toString match
        case "i" => action = Action.Interpret
        case "s" => action = Action.Transpile
        case _  => ()
    else
      inputPaths = inputPaths.prepended(s.toString)

  if inputPaths.isEmpty then
    throw IllegalArgumentException("no input")

  val source = SourceFile.withContentsOfFile(inputPaths.head) match
    case Success(f) => f
    case Failure(e) => throw e

  val c = driver.Configuration(IArray(source), traceInference)
  (action, c)

/** Applies `pipeline` with the given `c`, catching and reporting thrown exceptions. */
private def withConfiguration[T](
    c: driver.Configuration, pipeline: driver.Configuration => T
): Option[T] =
  try
    Some(pipeline(c))
  catch case e: DiagnosticSet =>
    e.log()
    None

/** Runs `alpinec`. */
@main def main(args: String*): Unit =
  val (a, c) = parseCommandLineArguments(args)
  a match
    case Action.Parse =>
      withConfiguration(c, driver.parse)
    case Action.TypeCheck =>
      withConfiguration(c, driver.typeCheck)
    case Action.Interpret =>
      withConfiguration(c, driver.interpret)
    case Action.Transpile =>
      withConfiguration(c, driver.transpile)
    case Action.Compile =>
      withConfiguration(c, driver.typeCheck)