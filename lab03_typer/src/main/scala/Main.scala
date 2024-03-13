import alpine.{DiagnosticSet, SourceFile}
import alpine.driver
import alpine.util.toSubstring

import scala.util.{Success, Failure}

/** The operation to perform on the compiler's input. */
private enum Action:
  case Parse, TypeCheck, Compile, Interpret

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
      if s.drop(1) == "i" then
        action = Action.Interpret
      else if s.drop(1) == "t" then
        action = Action.TypeCheck
      else if s.drop(1) == "p" then
        action = Action.Parse
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
      val t = (withConfiguration(c, driver.parse))
    case Action.TypeCheck =>
      val t = withConfiguration(c, driver.typeCheck)
      // if you want, you can print the type-checked tree here
