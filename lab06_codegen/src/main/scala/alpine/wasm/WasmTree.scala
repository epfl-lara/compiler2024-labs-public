package alpine.wasm

/** Represents WebAssembly instructions. */
object WasmTree:

  // --- Types ---------------------------------------------------------------
  sealed trait WasmType
  case object I32 extends WasmType:
    override def toString = "i32"
  case object I64 extends WasmType: // not used
    override def toString = "i64"
  case object F32 extends WasmType:
    override def toString = "f32"
  case object F64 extends WasmType: // not used
    override def toString = "f64"

  /** A Web Assembly module */
  case class Module(imports: List[Import], functions: List[Function]):
    /** Produces the WAT module definition. */
    def mkString(using d: Depth) =
      indent("(module") ++ "\n" ++
        imports.map(_.mkString(using d + 1)).mkString("\n") ++ "\n" ++
        functions.map(_.mkString(using d + 1)).mkString("\n") ++ "\n" ++
        indent(")")

  // --- Imports --------------------------------------------------------------
  sealed trait Import extends DepthFormattable

  /** Imports a function from the environment */
  case class ImportFromModule(
      module: String,
      name: String,
      importedName: String,
      params: List[WasmType] = Nil,
      returnType: Option[WasmType] = None
  ) extends Import:
    def mkString(using Depth) = indent(
      f"""(import "$module" "$name" (func $$$importedName ${generateParameterList(
          params
        )} ${generateResult(returnType)}))"""
    )
  
  /** Imports a memory from the environment */
  case class ImportMemory(
      module: String = "api",
      name: String = "mem",
      minPages: Int = 100
  ) extends Import:
    def mkString(using Depth) = indent(
      f"""(import "$module" "$name" (memory $minPages))"""
    )

  // --- Function ------------------------------------------------------------
  sealed trait Function extends DepthFormattable

  /** A function definition, not exported. */
  case class FunctionDefinition(
      name: String,
      params: List[WasmType] = Nil,
      locals: List[WasmType] = Nil,
      returnType: Option[WasmType] = None,
      body: List[Instruction]
  ) extends Function:
    def mkString(using d: Depth) =
      indent(
        f"(func $$$name ${generateParameterList(params)} ${generateResult(returnType)} ${generateLocals(locals)}"
      ) ++ "\n" ++
        body.map(_.mkString(using d + 1)).mkString("\n") ++ "\n" ++
        indent(")")

  /** A main function definition that is exported as “main”. */
  case class MainFunction(
      body: List[Instruction],
      returnType: Option[WasmType] = None
  ) extends Function:
    def mkString(using d: Depth) =
      FunctionDefinition(
        "main",
        returnType = returnType,
        body = body
      ).mkString ++ "\n" ++ indent("(export \"main\" (func $main))")

  // --- Instructions --------------------------------------------------------
  sealed trait Instruction extends DepthFormattable

  /** Instruction of the form `instName`. */
  sealed trait SimpleInstruction(val instName: String) extends Instruction:
    def mkString(using Depth) = indent(instName)

  /** Instruction of the form `instName arg`. */
  sealed trait UnaryInstruction[T <: Formattable](
      val instName: String,
      val arg: T
  ) extends Instruction:
    def mkString(using Depth) = indent(f"$instName ${arg.mkString}")

  /** A general if instruction. */
  sealed trait IfInstruction(
      val instName: String,
      val `then`: List[Instruction],
      val `else`: Option[List[Instruction]]
  ) extends Instruction:
    def mkString(using d: Depth) =
      List(
        indent(instName),
        `then`.map(_.mkString(using d + 1)).mkString("\n"),
        `else`
          .map(_.map(_.mkString(using d + 1)))
          .map(_.prepended("else").mkString("\n"))
          .getOrElse(""),
        indent("end")
      ).filter(!_.isEmpty()).mkString("\n")

  /** A general block instruction. */
  sealed trait BlockInstruction(
      val instName: String,
      val blockLabel: FormattableLabel,
      val instructions: List[Instruction]
  ) extends Instruction:
    def mkString(using d: Depth) =
      List(
        indent(f"$instName ${blockLabel.mkString}"),
        instructions.map(_.mkString(using d + 1)).mkString("\n"),
        indent("end")
      ).mkString("\n")

  // --- Constants
  /** Pushes a constant i32 to the stack */
  case class IConst(value: Int)
      extends UnaryInstruction("i32.const", FormattableInt(value))
  /** Pushes a constant f32 to the stack */
  case class FConst(value: Float)
      extends UnaryInstruction("f32.const", FormattableFloat(value))

  // Numerical i32 operations
  /** Consumes the top two values on the stack and pushes the sum */
  case object IAdd extends SimpleInstruction("i32.add")
  /** Consumes the top two values on the stack and pushes the substraction */
  case object ISub extends SimpleInstruction("i32.sub")
  /** Consumes the top two values on the stack and pushes the product */
  case object IMul extends SimpleInstruction("i32.mul")
  /** Consumes the top two values on the stack and pushes the (signed) division */
  case object IDiv extends SimpleInstruction("i32.div_s")
  /** Consumes the top two values on the stack and pushes the remainder */
  case object IRem extends SimpleInstruction("i32.rem_s")
  /** Consumes the top two values on the stack and pushes the bitwise and */
  case object IAnd extends SimpleInstruction("i32.and")
  /** Consumes the top two values on the stack and pushes the bitwise or */
  case object IOr extends SimpleInstruction("i32.or")
  /** Consumes the top two values on the stack and pushes 1 if they are equal, 0 otherwise. */
  case object IEqz extends SimpleInstruction("i32.eqz")
  /** Consumes the top two values on the stack and pushes 1 if the first one is smaller than the second one. (signed) */
  case object ILt_s extends SimpleInstruction("i32.lt_s")
  /** Consumes the top two values on the stack and pushes 1 if the first one is smaller or equal than the second one. (signed) */
  case object ILe_s extends SimpleInstruction("i32.le_s")
  /** Consumes the top two values on the stack and pushes 1 if the first one is equal to the second one. (signed) */
  case object IEq extends SimpleInstruction("i32.eq")
  /** Drops a value from the stack */
  case object Drop
      extends SimpleInstruction("drop")

  // Numerical f32 operations
  /** Consumes the top two values on the stack and pushes the sum */
  case object FAdd extends SimpleInstruction("f32.add")
  /** Consumes the top two values on the stack and pushes the substraction */
  case object FSub extends SimpleInstruction("f32.sub")
  /** Consumes the top two values on the stack and pushes the product */
  case object FMul extends SimpleInstruction("f32.mul")
  /** Consumes the top two values on the stack and pushes the division */
  case object FDiv extends SimpleInstruction("f32.div")
  /** Consumes the top two values on the stack and pushes 1 if they are equal, 0 otherwise. */
  case object FEq extends SimpleInstruction("f32.eq")
  /** Consumes the top two values on the stack and pushes 1 if the first one is smaller than the second one. */
  case object FLt extends SimpleInstruction("f32.lt")
  /** Consumes the top two values on the stack and pushes 1 if the first one is smaller or equal than the second one. */
  case object FLe extends SimpleInstruction("f32.le")

  // --- Control flow instructions ------------------------------------------
  /** An if “void” block */
  case class If_void(
      override val `then`: List[Instruction],
      override val `else`: Option[List[Instruction]] = None
  ) extends IfInstruction("if", `then`, `else`)
  /** An if block that returns an i32 */
  case class If_i32(
      override val `then`: List[Instruction],
      override val `else`: Option[List[Instruction]] = None
  ) extends IfInstruction("if (result i32)", `then`, `else`)
  /** Loop block. Branching to the label will jump to the beginning of the block. */
  case class Loop(label: String, override val instructions: List[Instruction])
      extends BlockInstruction("loop", FormattableLabel(label), instructions)
  /** Block loop. Branching to the label will jump after the block. */
  case class Block(label: String, override val instructions: List[Instruction])
      extends BlockInstruction("block", FormattableLabel(label), instructions)
  /** Branch, jumps to rhe label. */
  case class Branch(label: String)
      extends UnaryInstruction("br", FormattableLabel(label))
  /** Call instruction */
  case class Call(name: String)
      extends UnaryInstruction("call", FormattableLabel(name))
  /** NOP instruction: does nothing */
  object Nop extends SimpleInstruction("nop")
  /** Unreachable: unconditional trap (i.e. abort) */
  object Unreachable extends SimpleInstruction("unreachable")
  /** Return instruction */
  object `Return` extends SimpleInstruction("return")

  // Locals
  /** Pushes the local at the given index to the stack */
  case class LocalGet(index: Int)
      extends UnaryInstruction("local.get", FormattableInt(index))
  /** Sets the local at the given index to the value on the stack */
  case class LocalSet(index: Int)
      extends UnaryInstruction("local.set", FormattableInt(index))

  // --- Memory instructions ------------------------------------------------
  /** Stores an i32 to memory. Memory address and value are on the stack. */
  case object IStore extends SimpleInstruction("i32.store")
  /** Loads an i32 from memory. Memory address is on the stack. */
  case object ILoad extends SimpleInstruction("i32.load")

  // Byte memory
  /**
    * Stores a byte to memory. Memory address and value are on the stack.
    * The value is truncated to a byte with its least significant 8 bits.
    */
  case object IStore8 extends SimpleInstruction("i32.store8")
  /**
    * Loads a byte from memory. Memory address is on the stack.
    * Zero-extended to 32 bits, not sign-extended.
    */
  case object ILoad8 extends SimpleInstruction("i32.load8_u")

  /** A general if instruction that returns a result. */
  case class If_result(
      override val `then`: List[Instruction],
      override val `else`: Option[List[Instruction]] = None,
      result: WasmType
  ) extends IfInstruction(f"if (result ${result.toString})", `then`, `else`)

  /** A comment, useful for debugging */
  case class Comment(comment: String)
      extends UnaryInstruction(";;", FormattableText(comment))

  // --- Formattable util
  /** Depth is used to indent the output */
  opaque type Depth = Int
  given Depth = 0
  def indent(using d: Depth)(s: String) = "  " * d + s

  /** Generates the parameter list string for a function */
  def generateParameterList(params: List[WasmType]): String =
    if params.size > 0 then f"(param ${params.map(_.toString).mkString(" ")})"
    else ""

  /** Generates the result string for a function */
  def generateResult(result: Option[WasmType]): String =
    result match
      case Some(t) => f"(result ${t.toString})"
      case None    => ""

  /** Generates the locals string for a function */
  def generateLocals(locals: List[WasmType]): String =
    if locals.size > 0 then f"(local ${locals.map(_.toString).mkString(" ")})"
    else ""

  // Formattables
  /** Formattable with an identation level. */
  sealed trait DepthFormattable:
    def mkString(using Depth): String

  /** Formattable with a string representation. */
  sealed trait Formattable:
    def mkString: String
  case class FormattableInt(value: Int) extends Formattable:
    def mkString = value.toString
  case class FormattableFloat(value: Float) extends Formattable:
    def mkString = value.toString
  case class FormattableLabel(label: String) extends Formattable:
    def mkString = "$" + label
  case class FormattableText(text: String) extends Formattable:
    def mkString = text
