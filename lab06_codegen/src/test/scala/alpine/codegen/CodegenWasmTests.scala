package alpine.codegen

import scala.io.Source

class CodegenWasmTests extends munit.FunSuite:
  private val lineSeparator = System.lineSeparator()

  var runner: Option[CodegenWasmUtils.Runner] = None
  
  val inputFileAlpineTests = "./src/test/res/codegen/test_cases.al"

  /**
    * Parses the given file and run the obtained test cases
    *
    * @param filename
    * @param loc
    */
  def runTestsFromFile(filename: String)(implicit loc: munit.Location): Unit = {
    val lines: List[String] = Source.fromFile(filename).getLines().toList
    val tests = CodegenWasmUtils.parseTests(lines)
    for t <- tests do
      test(t.name) {
        val r = runner.get
        val alpineTestFilename = "Input.al"
        val inputAlpineFilePath = r.writeAlpineFile(alpineTestFilename, t.input.mkString(lineSeparator))
        val outputScalaFile = r.runAlpineCompiler(inputAlpineFilePath)
        val outputOfScala = outputScalaFile.flatMap(outputScalaFile => r.run(outputScalaFile).map(_.replace("\r\n", "\n")))
        outputOfScala match {
          case Right(output) =>
            val expected = t.expected.mkString(lineSeparator)
            assertEquals(output.stripSuffix("\n"), expected)
          case Left(error) =>
            throw error
        }
      }
  }

  override def beforeAll(): Unit =
    runner = Some(CodegenWasmUtils.Runner())

  override def afterAll(): Unit =
    runner.foreach(_.delete)

  runTestsFromFile(inputFileAlpineTests)