package alpine.wasm
import sys.process._

object Wasm:
  def wasmString(module: WasmTree.Module): String = module.mkString

  def writeToFile(path: String, module: WasmTree.Module): Unit =
    val writer = java.io.PrintWriter(path)
    writer.write(wasmString(module))
    writer.close()

  /** Transforms the given WebAssembly text file to a binary file. */
  def watToWasm(source: String, to: String, debugSymbols: Boolean = true): Boolean = 
    f"$wat2wasm $source -o $to${if debugSymbols then " --debug-names" else ""}".! == 0

  /** Runs with NodeJS the given WebAssembly file. */
  def runNode(wasm: String): Unit =
    f"$node ./wasm/node-runner.js $wasm".!

  /** Runs with NodeJS the given WebAssembly file, returning the stdout and stderr content and status code. */
  def runNodeCapturingStdOut(wasm: String): (Int, List[String], List[String]) =
    val output = new StringBuilder
    val err = new StringBuilder
    val logger = ProcessLogger(x => output.append(f"$x\n"), x => err.append(f"$x\n")) // Ignoring stderr
    val process = Process(f"$node ./wasm/node-runner.js $wasm").run(logger)
    val exitValue = process.exitValue()
    val stdout = output.toString.split("\n").toList.filter(_.contains("WASM returned: ")).map(_.replace("WASM returned: ", ""))
    val stderr = err.toString.split("\n").toList
    (exitValue, stdout, stderr)

  /** Runs with NodeJS the given WebAssembly file and waits for a debugger to attach. */
  def debugNode(wasm: String): Unit =
    f"$node ./wasm/node-runner.js $wasm wait-for-debugger".!

  /** Returns whether NodeJS is available on the local machine. */
  def isNodeAvailable = isBinAvailable(f"$node")
  /** Returns whether wat2wasm is available on the local machine. */
  def isWat2WasmAvailable = isBinAvailable(f"$wat2wasm")

  private def isBinAvailable(bin: String): Boolean =
    val result = Process(f"$which $bin")!(ProcessLogger(_ => ()))
    result == 0 

  private val which = if System.getProperty("os.name").toLowerCase().contains("windows") then "where.exe" else "which"
  private val npm = if System.getProperty("os.name").toLowerCase().contains("windows") then "npm.cmd" else "npm"
  private val node = if System.getProperty("os.name").toLowerCase().contains("windows") then "node.exe" else "node"
  private val wat2wasm = if System.getProperty("os.name").toLowerCase().contains("windows") then "wat2wasm.cmd" else "wat2wasm"
