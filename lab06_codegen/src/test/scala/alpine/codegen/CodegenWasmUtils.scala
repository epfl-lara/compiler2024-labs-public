package alpine.codegen

import alpine._
import sys.process._
import scala.io.Source
import java.nio.file.{ Files, Path }
import java.util.Comparator
import java.io.File
import alpine.wasm.Wasm


object CodegenWasmUtils:
   /**
    * Represents a test case for the code generator WASM
    *
    * @param name The name of the test, with the number of points as (Xpts) or (Xpt)
    * @param input The lines of the input file, i.e., the alpine code
    * @param expected The expected lines of the std out when running the transpiled program
    */
  case class CodegenWasmTest(name: String, input: List[String], expected: List[String])

  /**
    * Parses a file with the correct format and produces a list of TranspilerTest instances
    *
    * @param lines
    * @return
    */
  def parseTests(lines: List[String]): List[CodegenWasmTest] = 
    val indicesBeginningTests = lines.zipWithIndex.filter(p => p._1.startsWith("//BEGIN")).map(_._2)
    val indicesEndTests = lines.zipWithIndex.filter(p => p._1.startsWith("//END")).map(_._2 + 1)
    val boundsOfTests = indicesBeginningTests.zip(indicesEndTests)
    val testLists = boundsOfTests.map(p => lines.slice(p._1, p._2))
    testLists.map(l =>
      val name = l.head.replace("//BEGIN ", "")
      val codeEndIndex = l.tail.indexWhere(s => s.startsWith("//OUT"))
      val code = l.tail.slice(0, codeEndIndex).filter(!_.isEmpty())
      val out = l.slice(l.indexWhere(_.startsWith("//OUT")), l.indexWhere(_.startsWith("//END"))).filter(!_.contains("//OUT"))
      println(f"name = '$name'")
      println(f"code = $code")
      println(f"out = $out")
      CodegenWasmTest(name = name, input = code, expected = out)
    )

  

  /** Util class that spawns a temporary directory to run Scala files */
  class Runner:
    case class WasmRunError(message: String) extends Exception(message):
      override def toString: String = f"Failed to run Scala: \n$message"
    case class BackendError(exception: Throwable) extends Exception(exception):
      override def toString: String = f"Error from Alpine: \n$exception"

    val tmpDir = Files.createTempDirectory("codegenWasm")

    /** Runs the given Wasm file in the temporary directory */
    def run(input: Path): Either[WasmRunError, String] =
      val absolutePaths = input.toAbsolutePath()
      val (exitCode, output, stderr) = Wasm.runNodeCapturingStdOut(absolutePaths.toString())
      // 255 (-1) is reserved for panic
      if exitCode == 0 || exitCode == 255 then Right(output.mkString("\n"))
      else Left(WasmRunError("Exit code: " ++ exitCode.toString ++ "\n" ++ output.mkString("\n") ++ "\n-- stderr --\n" ++ stderr.mkString("\n")))


    /**
    * Run the alpine codegen to wasm, returning the Path to the generated .wat file
    *
    * @param inputFile
    * @return
    */
    def runAlpineCompiler(inputFile: Path): Either[BackendError, Path] =
      try
        val source = SourceFile.withContentsOfFile(inputFile.toAbsolutePath.toString).get
        val parsed = parsing.Parser(source).program()
        val typed = { val typer = typing.Typer(); typer.check(parsed) }
        val compiled = codegen.CodeGenerator(typed).compile()
        val watfileName = appendWatExtension("output")
        val wasmfileName = appendWasmExtension("output")
        val watfile = tmpDir.resolve(watfileName)
        val wasmfile = tmpDir.resolve(wasmfileName)
        Wasm.writeToFile(watfile.toString(), compiled)
        Wasm.watToWasm(watfile.toString(), wasmfile.toString())
        Right(wasmfile)
      catch (e: Throwable) =>
        Left(BackendError(e))

    /** Writes an Alpine file in the temporary directory. Prepends the .al if needed extension */
    def writeAlpineFile(name: String, content: String): Path =
      val file = tmpDir.resolve(appendAlpineExtension(name))
      Files.write(file, content.getBytes)
      file

    /** Deletes the temporary directory */
    def delete: Unit =
      Files.walk(tmpDir)
        .sorted(Comparator.reverseOrder())
        .map(_.toFile)
        .forEach(_.delete)

    private def appendWatExtension(filename: String): String =
      if filename.endsWith(".wat") then filename else f"$filename.wat"

    private def appendWasmExtension(filename: String): String =
      if filename.endsWith(".wasm") then filename else f"$filename.wasm"

    private def appendAlpineExtension(filename: String): String =
      if filename.endsWith(".al") then filename else f"$filename.al"
