# Lab 05 - Transpiler

In this fourth lab, you will implement a transpiler from Alpine to Scala.
Recall that a transpiler is a compiler that translates code from one programming language to another language meant to be read and modified by humans.

## Obtaining the lab files

As in the previous labs, you can obtain the new lab files by cloning the repository or downloading the ZIP file from Moodle. Two ways to proceed:

- **Cloning the repository**: Clone the repository and put back the files you have modified in the previous labs. (`src/main/scala/alpine/typing/Typer.scala`, `src/main/scala/alpine/parser/Parser.scala`, `src/main/scala/alpine/interpreter/Interpreter.scala`).
- **Copying only new files**: If you don't want to clone the repository again, you can download the ZIP file from Moodle and copy the new files to your project.

The new files/updated files are:
- `scala_rt/*`
- `src/main/scala/alpine/driver/Driver.scala`
- `src/main/scala/alpine/codegen/ScalaPrinter.scala`
- `src/main/scala/Main.scala`
- `src/main/scala/symbols/Entities.scala`
- `src/main/scala/symbols/EntityReference.scala`
- `src/main/scala/symbols/Types.scala`
- `src/main/scala/typing/ProofObligations.scala`
- `src/main/scala/typing/Typer.scala` (please see below.)
- `src/test/res/*` (new folder containing files)
- `src/test/scala/alpine/transpiler/*` (new folder containing files)

### `Typer.scala` modification

There is an update for the `Typer.scala` file:

```scala
  private def commit(solution: Solver.Solution, obligations: ProofObligations): Unit =
    for (n, t) <- obligations.inferredType do
      val u = solution.substitution.reify(t)
      val v = properties.checkedType.put(n, u)
      assert(v.map((x) => x == u).getOrElse(true))

      // The cache may have an unchecked type for `n` if it's a declaration whose type has been
      // inferred (i.e., variable in a match case without ascription).
      properties.uncheckedType.updateWith(n)((x) => x.map((_) => Memo.Computed(u)))

    for (n, r) <- solution.binding do
      val s = symbols.EntityReference(r.entity, solution.substitution.reify(r.tpe))
      properties.treeToReferredEntity.put(n, s)

    reportBatch(solution.diagnostics.elements)
    assert(solution.isSound || diagnostics.containsError, "inference failed without diagnostic")
```

becomes

```scala
  private def commit(solution: Solver.Solution, obligations: ProofObligations): Unit =
    for (n, t) <- obligations.inferredType do
      val u = solution.substitution.reify(t)
      val v = properties.checkedType.put(n, u)
      assert(v.map((x) => x == u).getOrElse(true))

      // The cache may have an unchecked type for `n` if it's a declaration whose type has been
      // inferred (i.e., variable in a match case without ascription).
      properties.uncheckedType.updateWith(n)((x) => x.map((_) => Memo.Computed(u)))

    for (n, r) <- (obligations.inferredBinding ++ solution.binding) do
      val s = r.withTypeTransformed((t) => solution.substitution.reify(t)) // ← This line changes.
      properties.treeToReferredEntity.put(n, s)

    reportBatch(solution.diagnostics.elements)
    assert(solution.isSound || diagnostics.containsError, "inference failed without diagnostic")
```

and

```scala
  private def bindEntityReference(
      e: ast.Tree, candidates: List[symbols.EntityReference]
  )(using context: Typer.Context): Type =
    candidates match
      case Nil =>
        context.obligations.constrain(e, Type.Error)
      case pick :: Nil =>
        properties.treeToReferredEntity.put(e, pick)
        context.obligations.constrain(e, pick.tpe)
      case picks =>
        val t = freshTypeVariable()
        context.obligations.add(Constraint.Overload(e, picks, t, Constraint.Origin(e.site)))
        context.obligations.constrain(e, t)
```

becomes

```scala
  private def bindEntityReference(
      e: ast.Tree, candidates: List[symbols.EntityReference]
  )(using context: Typer.Context): Type =
    candidates match
      case Nil =>
        context.obligations.constrain(e, Type.Error)
      case pick :: Nil =>
        context.obligations.bind(e, pick) // ← This line changes.
        context.obligations.constrain(e, pick.tpe)
      case picks =>
        val t = freshTypeVariable()
        context.obligations.add(Constraint.Overload(e, picks, t, Constraint.Origin(e.site)))
        context.obligations.constrain(e, t)
```

## Test dependencies

The test suite has to compile the generated Scala code for testing, therefore, please make sure that the two commands below work in your shell before starting the lab:

```bash
$ scalac -version
$ scala -version
```

If you have installed Scala with Coursier, it should be already set up. If you have installed Scala with another method, you should make sure that these commands work.

## Transpiling Alpine to Scala

Like the interpretation and type checking/inference, transpilation is implemented as a AST traversal using `TreeVisitor`.
In a nutshell, the process consists of walking the AST and, for each node, generate Scala code to produce equivalent semantics.

Most constructs from Alpine have a straightforward equivalent in Scala.
For example, let's examine how conditional expressions (e.g., `if a then b else c`), which are processed by `visitConditional`:

```scala
override def visitConditional(n: ast.Conditional)(using context: Context): Unit =
  context.output ++= "if "
  n.condition.visit(this)
  context.output ++= " then "
  n.successCase.visit(this)
  context.output ++= " else "
  n.failureCase.visit(this)
```

The construction of the Scala program is done by appending strings to a string buffer named `output`, which is part of the context in which the AST traversal takes place.

> In Scala, using a string buffer is more efficient than concatenating many small strings together to construct a large string.
> You append a new string to the buffer with the operator `++=`.
> Once it has been constructed, you extract the final by calling `toString` on the buffer.
> Note that you can extract the contents of the buffer at any time using the same method, which may be useful for debugging in an interactive session or via `println` statements.

The output of visiting a conditional is therefore the following:

```scala
if <n.condition.visit> then <n.success.visit> else <n.failure.visit>
```

This code snippet demonstrates the convenience of the visitor pattern.
One simply has to call the `visit` method of a sub-tree to transpire its contents into the buffer.
Hence, most transpilation methods, like `visitConditional`, read like flat sequences of relatively straightforward statements.

We provide an incomplete implementation of the `ScalaPrinter` class.
Your task is to implement transpilation for **pattern matching**, **records**, **narrowing**, and **widening**, performed by methods `visitMatch`, `visitRecord` and `visitAscription`.

In this lab, we, on purpose, let you more freedom about how you implement the transpiler. The representation in Scala of the records is up to you. The only specification is that the generated Scala code should implement correctly the behavior of the Alpine code when run.

But before all of that, a little bit of explanations:

### Built-in features and runtime libraries

The code generated by compilers is not necessarily standalone, and can therefore depend on some libraries. For instance, Scala has its standard library (e.g. `::`, …), C has the `libc` (e.g. `printf`, `strcpy`, [`malloc`](https://github.com/bminor/glibc/tree/master/malloc) …), Java has the `java.lang` package, etc...

Sometimes, some features of the language are implemented in such standard libraries.

For instance, throwing exceptions in C++ with `gcc`:

```cpp
// Throws an exception
throw "Division by zero";
```

is compiled to a call to a runtime function:

```cpp
// Throws an exception
__cxa_throw("Division by zero", "char const*", 0);
```

The details are not important but the idea is that the generated code can depend on _other_ code that is not directly generated by the compiler (but is still compiled at some point, as its own program), to support specific features of the language (here, `__cxa_throw` is a function imported from a library at runtime).

Alpine also has its standard library that will be shipped with the transpiled code to support some features of the language. For example, this library implement `print`, `+`, `@`, ...

To summarize,  The transpiler should generate code and _can_ rely on a runtime library that will be available at runtime to implement some features of the language.

### Issue #1: Scala and Alpine differences

Scala and Alpine are two different languages. One of the crucial differences is the presence of _labels_! In Scala, labels do not exist. In Alpine, they do.

For instance, in Alpine, you can write:

```swift
fun myFun(_ x: Int) -> Int { 5 }
fun myFun(a x: Int) -> Int { 10 }
myFun(2) // 5, calls the 1st function
myFun(a: 2) // 10, calls the 2nd function
```

In this case, the two functions are two **different** fucntions.

The same applies to records. In Alpine, you can write:

```swift
let p = #point(x: 1, y: 2)
let main = match p {
  case #point(1, 2) then print("Unreachable, sorry.") // not a subtype of #point(x: Int, y: Int)
}
```

This means that the translation to Scala cannot be as straightforward than it is for conditional for example. You need to find a way to take these labels into account, and to encode them in some way in the generated Scala code.

### Your task

Your task is to implement the missing implementations of the `visit` function, namely for what is related to **records**, **pattern matching**, **narrowing** and, **widening**. If necessary, complete the runtime library to support these new features.

### Hints

- Check the *provided* code, as you can take inspiration from existing implementations.
- A record, depending on its arity, is either a `case object` or a `case class`.
- The Scala `.asInstanceOf[T]` and `.isInstanceOf[T]` methods can be useful.
- You can take inspiration of the functions treatment for records.
- For pattern matching, there is a few cases to consider:

```swift
let p = #point(x: 1, y: 2)
let xCheck = 1
let main = match p {
  case #point(x: xCheck, y: 2) then print("Correct!") // executes
}
```

But cases such as:

```swift
let p = #point(x: 1, y: 2)
let xCheck = 1
let main = match p {
  case #point(x: xCheck + 1, y: 2) then print("Correct!") // executes
}
```

are not going to be checked (because of the way the pattern matching is implemented in Scala.)

A way to do it in Scala would be:

```scala
case class Point(x: Int, y: Int)
val p = Point(1, 2)
val xCheck = 1
val main = p match {
  case Point(`xCheck`, 2) => println("Correct!")
}
```