
# Lab 01 - Interpreter

Welcome to the Compiler course 2024!

In this first lab, you will implement an interpreter for the Alpine language, the language for which we will write the entire compiler during the semester.

**This lab is due on Friday 1st of March at 7pm**.

## Preparing the environment

We will use the following tools, make sure to install them before starting the lab:

* `scala`, `sbt`, …: The whole project will be done in Scala. Hence, you need to have Scala and sbt installed on your machine. You can see a tutorial for installing the toolchain [here](https://docs.scala-lang.org/getting-started/index.html).
* If you use Visual Studio Code, you can install the `Metals` extension to have a better experience with Scala. You can find an up-to-date tutorial to install it [here](https://scalameta.org/metals/docs/editors/vscode/).
* If you use IntelliJ, you can install the Scala plugin to have a better experience with Scala. You can find an up-to-date tutorial to install it [here](https://www.jetbrains.com/help/idea/get-started-with-scala.html).
* If you use another editor, feel free to check [Metals' documentation](https://scalameta.org/metals/docs/) to see if there is a plugin for your editor.

### Common issues

#### No autocompletion on Visual Studio Code (VSC)

If the autocompletion does not work, check the following on VSC:

* Metals is indeed installed.
* `build.sbt` file is at the root of your project. This means that the folder that you open inside Visual Studio Code is the folder that contains the `build.sbt` file.
* Make sure to import the build inside Metals. You can do it by either pressing `Ctrl+Shift+P` and typing `Import build` or clicking on the "Metals" tab in the left panel and then clicking on the "Import build" under the "Build commands" panel.
  * A loading icon should appear at the bottom-right and should disappear after a few seconds. If it does not, you can click on the icon and see what is the issue.
  * If it does not work, you can select "Clean and restart build server" and click on "Reset workspace" in the pop up.
  * If the issue persists, you can try to restart Visual Studio Code.
  * If it still does not work, you can see the logs by clicking on the "Check logs" button under the "Help and feedback" pane in the "Metals" tab to inspect it.

## Obtaining the lab files

To get the lab files, clone this repository. To do so, open a terminal and run the following command:

```console
$ git clone https://github.com/epfl-lara/compiler2024-labs-public.git
```

Then the `interpreter` folder contains this week lab files.

## Submit your work

We are using an automatic grading infrastructure to grade your work. You will submit your work on Moodle and automatically receive your grade after a few minutes. To submit your work, go to this week assignment on Moodle and upload the following file:

* `src/main/scala/alpine/evaluation/Interpreter.scala`

When you submit your work, it will create a "Draft" submission. You then have to click on the "Submit" button to submit your work. DO NOT FORGET TO DO THIS STEP! You will receive an email confirming that your work has been submitted. If you do not receive this email, it means that your work has not been properly submitted.

## General idea of the project

Let's recall the global idea of a simple compiler's pipeline:

```
Source code -> Lexer -> Parser -> Type checking -> Assembly generation
```

To recap,

* The `Lexer` is responsible for transforming the source code into a list of tokens (words, numbers, symbols, etc.). It is the first step of the pipeline.
* The `Parser` is responsible for transforming the list of tokens into an abstract syntax tree (AST). It is the second step of the pipeline.
* The `Type checking` is responsible for checking that the program is well-typed. It is the third step of the pipeline.
* The `Assembly generation` is responsible for generating the machine code. It is the last step of the pipeline.

This week, we will not do a *compiler* but an *interpreter*. The difference is that we will not generate machine code but execute the program directly. Hence, the pipeline is simplified:

```
Source code -> Lexer -> Parser -> Type checker -> Interpreter
```

Here, the `Interpreter` is responsible for executing the program. It is the last step of the pipeline. Note that the interpreter interprets a `TypedProgram` and not a `Program`. This means that the type checker is run on the AST to solve references to types and variables.

### Lab formalities

In this lab, you will modify only the `evaluation/Interpreter.scala` file. You can obviously check other files to have a better understanding of the project, but you should not modify them.

To run the program, you can use the following SBT command (inside the `sbt` terminal launched from the root of the project, i.e., where the `build.sbt` file is located):

```console
run -i <input_file>
```

This will run the interpreter on the file `<input_file>` (an Alpine source file).

We provide you a test suite (comprising unit, integration, and end-to-end tests) to check your work. To run it use the following SBT command:

```console
test
```

These also are the tests on which your work will be graded.

### _Alpine_ language

You can find a description of the language inside the [`language_desc.md`](./language_desc.md)/['language_desc.html`](./language_desc.html) file. It contains a small overview of the language, its syntax, and its semantics.

### The AST

Let's recall the pipeline seen above. The `Parser` is responsible for transforming the list of tokens into an Abstract Syntax Tree (AST).

The interpreter will traverse the AST produced by the parser and execute the program.

The AST is a tree that represents the structure of the program. It is a data structure that is used to represent the program in a way that is easy to manipulate. For instance, the following program:

```
let x = 1
```

is represented by the following AST:

```
IArray(
  Binding(
    x, // identifier
    None, // explicit type on the definition
    Some( // initialisation content (i.e. the 1), if exists
      IntegerLiteral(
        1, // literal value
        <input>:1:9-1:10 // position of the literal
      )
    ), 
    <input>:1:1-1:10 // position of the binding
  )
)
```

If you have taken the CS-214 Software Construction course: yes, it looks similar to the evaluator we have seen, though more complete (and therefore complex).

You can find the definitions of all the types of the AST in the `alpine.ast.Trees` file (`src/main/scala/alpine/ast/Trees.scala` file.)

## Implementing the interpreter

You will implement the interpreter in the `alpine.evaluation.Interpreter` class (`src/main/scala/alpine/evaluation/Interpreter.scala` file.)

### Using SBT

To run files with the interpreter, you can use the following SBT command (inside the `sbt` terminal launched from the root of the project, i.e., where the `build.sbt` file is located):

```
run -i <input_file>
```

To test your code, you can use the following SBT command:

```
test
```

### Step 0: Entrypoint (provided)

The entrypoint of an *Alpine program* is its `main` variable (i.e. variable called `main`.) The interpreter has to find the expression contained in the `main` variable and interpret it.

Inside the interpreter class, you will find the `run` method that is the entry point of the *interpreter*. The variable of type `TypedProgram` contains the AST of the *Alpine* program to interpret.

<div class='snippet'>

```scala
/** Evaluates the entry point of the program. */
def run(): Int =
  val e = syntax.entry.getOrElse(throw Panic("no entry point"))
  try
    e.visit(this)(using Context())
    0
  catch case e: Interpreter.Exit =>
    e.status
```

<p class='snippet-path'>src/main/scala/alpine/evaluation/Interpreter.scala</p>
</div>


Note that the `run` method is already implemented. It calls the `visit` method on the expression contained in the `main` variable which can be retrieved by calling the `entry` method on the `TypedProgram` instance.

The general idea is that the interpreter will successively visit the nodes of the AST and execute the program while visiting.

### Step 1: Parenthesized expressions

A parenthesized expression is an expression that is enclosed in parentheses. In _Alpine_, a parenthesized expression is written as `(<expression>)`. The interpreter should evaluate parenthesized expressions when it visits a `ParenthesizedExpression` node. To do so, implement the function `visitParenthesizedExpression` in the `Interpreter` class, that should evaluate to the value of the expression.

### Step 2: Records

A record is a collection of fields. Each field has a name and a value, which can be of any type. The record type is a way to group several values together. See it as a `data class` in Kotlin, a `struct` in C, a `case class` in Scala, a `record` in Java, etc.

In _Alpine_, a record (e.g. `#pair(x: 1, y: 2)`) is uniquely identified by:

* its name (i.e. identifier, e.g. `pair` for the record `#pair`)
* its _arity_ (i.e. the number of fields it has, e.g. here `#pair` has 2)
* the types of the fields it contains (e.g. here `Int` and `Int`)
* its field labels (e.g. here `x` and `y`)

The interpreter should be able to create record instances when it visits a `Record` node. To do so, implement the function `visitRecord` in the `Interpreter` class, that should evaluate to a `Record` value.

### Step 3: Conditional expressions

A conditional expression is an expression that evaluates to a value depending on a condition. In _Alpine_, a conditional expression is written as `if <condition> then <then-branch> else <else-branch>`. The interpreter should evaluate conditional expressions when it visits a `Conditional` node:

* evaluate/visit the `condition`
* if the condition evaluates to `true`, evaluate/visit the `then-branch` (`successCase`) and return its value
* if the condition evaluates to `false`, evaluate/visit the `else-branch` (`failureCase`) and return its value

You can now implement the function `visitConditional` in the `Interpreter` class.

### Step 4: Ascribed expressions

An ascribed expression is an expression that is annotated with a type. In _Alpine_, an ascribed expression is written as `<expression> [@ | @! | @?] <type>`. The interpreter should evaluate ascribed expressions when it visits an `AscriptionExpression` node.

There is multiple ways of changing the type of an expression:

1. **Widening**: when the type of the expression is a subtype of the ascribed type (). For example, `42 @ Int` is valid because `42` is a subtype of `Int`. Moreover, `42 @ Any` is valid because `Int` is a subtype of `Any`. This is called _widening_ and is always safe and valid.
2. **Unconditional narrowing**: Narrowing is less safe. It is when the type of the expression is a supertype of the ascribed type. For example, `42 @! Float` is invalid because `Int` is not a supertype of `Float`. `let x: Int | Float = 42; x @! Int` on the other hand is valid, because `Int | Float` is a supertype of `Int`. This is called _narrowing_ and is not always safe and valid.
In _Alpine_, narrowing can be done using the `@!` operator. If the ascribed type is not a subtype of the type of the expression, the interpreter should raise a `Panic`.
3. **Safe narrowing**: Instead of forcing such a narrowing, one can use the `@?` operator. It returns either a `#none` or a `#some(T)` depending on whether the narrowing is valid or not.

For example:
  ```swift
  let x = 42 @ Any
  let y = x @? Int
  ```
  `y` will evaluate to `#some(42)`. If the ascribed type is not a subtype of the type of the expression, the result is `#none`, e.g.
  ```swift
  let x = 42 @ Any
  let y = x @? Float
  ```
  `y` will evaluate to `#none`.

Implement now the function `visitAscribedExpression` in the `Interpreter` class:

* In case of a widening, return the value of the expression
* In case of an unconditional narrowing, check if the ascribed type is a subtype of the type of the expression. If it is, return the value of the expression. Otherwise, raise a `Panic`.
* In case of a safe narrowing, return `#some(value)` if the ascribed type is a subtype of the type of the expression, and `#none` otherwise.

Note that `#none` and `#some` are built-in records, and are defined in the `Value` object, in the `evaluation/Value.scala` file.

### Step 5: Functions

A function is a block of code that can be called. In _Alpine_, a function is defined using the `fun` keyword. For example, the following code defines a function `add` that takes two arguments `x` and `y` and returns their sum:

```swift
fun add(_ x: Int, _ y: Int) -> Int {
  x + y
}
```

Note that by construction of our interpreter, it will never visit a `Function` node (since we are interpreting directly the entrypoint.)

However, we may encounter the definition of functions with a `Let`/`Biding` node:

```swift
let add = (_ x: Int, _ y: Int) -> Int {
  x + y
}
```

Such functions are called lambda functions (or anonymous functions). The interpreter should evaluate lambda functions when it visits a `Let` node. Moreover, these lambda functions can be defined inside a function in itself and has capture semantics. For example:

```swift
fun adder(_ x: Int, _ y: Int) -> Int {
  let add = (_ z: Int) -> Int {
    x + z // x is captured from the outer scope
  } {
    add(y)
  }
}
let main = print(adder(3, 4))
```

has the same behavior as the previous `add` function.

Here, the lambda captures the `x` variable from the outer scope. This is what the lambda "captured".

The `_` in the function definitions are optional argument names. They can be used to make the code more readable by giving names to the arguments for interface/documentation purposes, that are are different from the variable names used in the body.

For instance:

```swift
fun scale(by f: Int) -> Int {
  10 * f
}
let main = print(scale(by: 2))
```

It lets the programer give more information about the argument to the caller, without having to use this name inside the function.

When the wildcard `_` is used, you can not specify the name of the argument when calling the function. For instance, the following code is invalid:

```swift
fun scale(_ f: Int) -> Int {
  10 * f
}
let main = print(scale(f: 2))
```

This is the correct way to call the function:

```swift
fun scale(_ f: Int) -> Int {
  10 * f
}
let main = print(scale(2))
```

#### Step 5.1: Applications

An application is the act of calling a function. In _Alpine_, we differentiate applications in three categories:

1. Standard function call (`Application`): e.g. `add(1, 2)`
2. Binary operator call (`InfixApplication`): e.g. `1 + 2` which is equivalent to `iadd(1, 2)` (`iadd` is a built-in function)
3. Unary operator call (`PrefixApplication`): e.g. `-1` which is equivalent to `ineg(1)` (`ineg` is a built-in function)

You should evaluate first:

* the function i.e., retrieve the function node from the context using the identifier
* the arguments i.e., evaluate/visit the arguments (call by value semantic)

Then, you should call the function on the arguments: you should use the `call` helper function.

#### Step 5.2: `call` function

Let's implement the `call` function.

The implementation for all the built-in functions is provided, you can take inspiration from it. 

Your task is to implement the function for the following cases:

* the function is not a built-in function and is not a lambda function
* the function is not a built-in function but is a lambda function

In each case, the `context` needs to be updated with the new bindings.

<div class="warn">

In lambdas, do not forget about the captures!

</div>

### Step 6: Let bindings

A let binding is how to define a new variable with a new block. We are already familiar with the top-level let binding (`Binding` node) but let's see the `Let` node.

In _Alpine_, a let binding is written as:

```swift
let x = 1 {
  x + 1
}
```

and here evaluates to `2`.

In other words, a `Let` is a `Binding` with a body that is executed with the new binding in the context.

To do so, implement the function `visitLet` in the `Interpreter` class, that should:

* evaluate/visit the `definition` of the `Binding`.
* evaluate/visit the body of the `Let` with the new `Binding`.

Note that the `Let` has call-by value semantics (i.e. the definition of the `Binding` is evaluated before evaluating the body of the `Let` and every subsequent reference to that new variable makes reference to the value to which the definition was evaluated.)

### Step 7: Pattern matching

Pattern matching is a way to match a value against a pattern. In _Alpine_, a pattern matching looks like:

```swift
match <scrutinee> {
  case <pattern> then <expression>
  // …
}
```

For instance,

```swift
match #person(age: 1) {
  case #person(age: 1) then 1
  case #person(age: 2) then 2
  case _ then -1
}
```

`#person(age: 1)` is the scrutinee and `#person(age: 1)`, `#person(age: 2)` and `_` are the patterns. Here, the expression evaluates to `1`.

A pattern can be recursively constructed with the following elements:

* A wildcard (`_`): matches any value
* An exact value: matches the exact value
* A record: matches a record with the same name and the same fields

Moreover, bindings can be defined in the patterns. For instance:

```swift
match someValue {
  case let p: #person(age: Int) then p.age >= 2
  case _ then false
}
```

In this case, the first expression of the first case is evaluated with a new binding: `p` is bound to the record `#person` of type `#person(age: Int)` (i.e., here, the scrutinee value).

For instance, if `someValue` was `#person(age: 1)`, the first case would be executed and the expression would evaluate to `false` (because `1 >= 2` evaluates to false because `p.age = 1`.) If `someValue` was not a `#person` record, the second case would be executed and the expression would evaluate to `false`.

Similarly, you can have `let` inside records too:

```swift
match someValue {
  case #person(age: let n) then n >= 2
  case _ then false
}
```

and this has the same behavior as the previous example.

Similar to the `Let` node, the `Match` node has call-by value semantics (i.e., the scrutinee is first evaluated to a value before executing the `match`).

If the scrutinee does not match any pattern, the interpreter should raise a `Panic`.

If the scrutinee matches multiple patterns, the first pattern that matches is chosen.

Note as well that patterns can contain expressions. For instance, the following code is valid:

```swift
match #person(age: 2) {
  case #person(age: 1) then 1
  case #person(age: 1 + 1) then 2
  case _ then -1
}
```

and evaluates to `2`.

So expressions in patterns should be evaluated before matching.

#### Step 7.1: Visiting the `Match` node

To implement the pattern matching, implement the helper function `visitMatch` in the `Interpreter` class, that should:

* evaluate/visit the `scrutinee`
* evaluate/visit the `cases` and return the value of the first case that matches the scrutinee

You can use the `matches` function to check if a pattern matches a value. `matches` returns either `None` if the pattern does not match the value, or `Some(bindings)` if the pattern matches the value. The `bindings` is a map from the variable identifiers to the values they are bound to, and of type `Frame` (that is a `Map[symbols.Name, Value]`.)

These bindings should be used to evaluate the case return expression.

#### Step 6.2: Implementing `matches`

`matches` makes a call to either:

* `matchesWildcard`
* `matchesValue`
* `matchesRecord`
* `matchesBinding`

depending on the type of the pattern.

* `matchesWildcard` matches any value and returns an empty `Frame`
* `matchesValue` matches the exact value and returns an empty `Frame`
* `matchesBinding` matches a binding and returns a `Frame` with the bindings if any. Be careful, the type of the binding must match as well.
* `matchesRecord` matches a record with the same name and the same fields, and returns a `Frame` with the bindings if any. Note that the fields of the record can be expressions and that the patterns for the fields should be recursively matched. Be careful, the record type must match as well (you can use `pattern.tpe` and `structurallyMatches`.)

### Step 8: Lambdas

We have seen lambda functions. However, we have not implemented the `visitLambda` method.

Implement the function `visitLambda` in the `Interpreter` class, that should:

* return a `Lambda` value
* update the `context` with the new bindings and specify a new `Frame` for the captures.

<div class="hint">

As a reminder, the captures are the variables that are used in the lambda but are not defined in the lambda (i.e., whose values comes from the environment outside of the lambda). For instance, in the following code:

```swift
let x = 1
let add = (_ y: Int) -> Int {
  x + y
}
```

`x` is a capture of the `add` lambda.

Check the `Context.flattened`method while implementing the captures.

</div>

### Grammar

 In this section, we give a list of built-in functions and a summary of the tree types you will work with.

#### Built-in functions

Here is a list of all the built-in functions, provided for reference:

* `equality(a: Any, b: Any) -> Bool`: returns `true` if `a` and `b` are equal, `false` otherwise (`a == b`)
* `inequality(a: Any, b: Any) -> Bool`: returns `true` if `a` and `b` are not equal, `false` otherwise (`a != b`)
* `lnot(a: Bool) -> Bool`: returns `true` if `a` is `false`, `false` otherwise (`!a`)
* `land(a: Bool, b: Bool) -> Bool`: returns `true` if `a` and `b` are `true`, `false` otherwise (`a && b`)
* `lor(a: Bool, b: Bool) -> Bool`: returns `true` if `a` or `b` is `true`, `false` otherwise (`a || b`)
* `ineg(a: Int) -> Int`: returns the negation of `a` (`-a`)
* `iadd(a: Int, b: Int) -> Int`: returns the sum of `a` and `b` (`a + b`)
* `isub(a: Int, b: Int) -> Int`: returns the difference of `a` and `b` (`a - b`)
* `imul(a: Int, b: Int) -> Int`: returns the product of `a` and `b` (`a * b`)
* `idiv(a: Int, b: Int) -> Int`: returns the division of `a` by `b` (`a / b`)
* `irem(a: Int, b: Int) -> Int`: returns the remainder of `a` by `b` (`a % b`, commonly known as modulo.)
* `ishl(a: Int, b: Int) -> Int`: returns the result of the left shift of `a` by `b` (`a << b`)
* `ishr(a: Int, b: Int) -> Int`: returns the result of the right shift of `a` by `b` (`a >> b`)
* `ilt(a: Int, b: Int) -> Bool`: returns `true` if `a` is strictly less than `b`, `false` otherwise
* `ile(a: Int, b: Int) -> Bool`: returns `true` if `a` is less than or equal to `b`, `false` otherwise
* `igt(a: Int, b: Int) -> Bool`: returns `true` if `a` is strictly greater than `b`, `false` otherwise
* `ige(a: Int, b: Int) -> Bool`: returns `true` if `a` is greater than or equal to `b`, `false` otherwise
* `iinv(a: Int) -> Int`: returns the bitwise inversion of `a` (`~a`)
* `iand(a: Int, b: Int) -> Int`: returns the result of the bitwise and of `a` and `b` (`a & b`)
* `ior(a: Int, b: Int) -> Int`: returns the result of the bitwise or of `a` and `b` (`a | b`)
* `ixor(a: Int, b: Int) -> Int`: returns the result of the bitwise xor of `a` and `b` (`a ^ b`)
* `fneg(a: Float) -> Float`: returns the negation of `a` (`-a`)
* `fadd(a: Float, b: Float) -> Float`: returns the sum of `a` and `b` (`a + b`)
* `fsub(a: Float, b: Float) -> Float`: returns the difference of `a` and `b` (`a - b`)
* `fmul(a: Float, b: Float) -> Float`: returns the product of `a` and `b` (`a * b`)
* `fdiv(a: Float, b: Float) -> Float`: returns the division of `a` by `b` (`a / b`)
* `flt(a: Float, b: Float) -> Bool`: returns `true` if `a` is strictly less than `b`, `false` otherwise (`a < b`)
* `fle(a: Float, b: Float) -> Bool`: returns `true` if `a` is less than or equal to `b`, `false` otherwise (`a <= b`)
* `fgt(a: Float, b: Float) -> Bool`: returns `true` if `a` is strictly greater than `b`, `false` otherwise (`a > b`)
* `fge(a: Float, b: Float) -> Bool`: returns `true` if `a` is greater than or equal to `b`, `false` otherwise (`a >= b`)

#### Tree types and summary

Here is a summary of the tree types you will work with, defined in the `ast` package.

* `ParenthesizedExpression(inner: Expression, …)`: `(<expression>)`, evaluates to `<expression>`.
  * The inner expression is `inner`.
  * Example: `((1 + 2))` → `((3))` → `(3)` → `3`
* `Record(identifier: String, fields: List[Labeled[Expression]], …)`: 
  * `<identifier>`, evaluates to a singleton record with the given name. (`fields = Nil`)
  * `<identifier>(<field1>: <expression1>, …)`, evaluates to a record with the given fields and values.
  * `<identifier>` starts with `#`.
  * Example: `#pair(x: 1, y: 2)`, `#singleton`, `#none`
* `Conditional(condition: Expression, successCase: Expression, failureCase: Expression, …)`: 
  * `if <condition> then <successCase> else <failureCase>`, evaluates to `<successCase>` if `<condition>` evaluates to `true`, `<failureCase>` otherwise.
  * Example: `if true then 1 else 2` → `1`, `if false then 1 else 2` → `2`
* `AscribedExpression(inner: Expression, operation: Typecast, ascription: Type, …)`:
  * `inner` is the expression to be typecasted.
  * `@` means `operation = Typecast.Widen`
    * Returns the value of the expression. The check is left to the type-checker that is provided.
    * Examples: `1 @ Int`, `1 @ Any`
  * `@!` means `operation = Typecast.NarrowUnconditionally`
    * Returns the value of the expression if the ascribed type is a subtype of the type of the expression. Otherwise, raises a `Panic`.
    * Examples: `1 @! Float`
  * `@?` means `operation = Typecast.Narrow`
    * Returns `#some(value)` if the ascribed type is a subtype of the type of the expression, and `#none` otherwise.
    * Examples: `1 @? Int`, `1 @? Float`
* `Application(function: Expression, arguments: List[Labeled[Expression]], …)`: `<function>(<arguments>)`
  * `function` is the function to be called.
  * `arguments` is the list of arguments.
  * Example: `iadd(1, 2)`
* `PrefixApplication(function: Expression, argument: Expression, …)`: `<operator> <expression>`
  * `function` is the function to be called.
  * `argument` is the argument.
  * Example: `-1`
  * The `function` is resolved when parsing from `<operator>`.
* `InfixApplication(function: Expression, lhs: Expression, rhs: Expression)`: `<lhs> <operator> <rhs>`
  * `function` is the function to be called.
  * `lhs and `rhs are the arguments.
  * Example: `1 + 2`
  * The `function` is resolved when parsing from `<operator>`.
* `Binding(identifier: String, ascription: Option[Type], initializer: Option[Expression], …)`:
  * `identifier` is the name of the variable.
  * `ascription` is the type of the variable, if given.
  * `initializer` is the initial value of the variable, if given.
  * Possible syntaxes:
    * `let <identifier> = <expression>`
    * `let <identifier>: <ascription>`
    * `let <identifier>: <ascription> = <expression>`
* `Let(binding: Binding, body: Expression, …)`: `<binding> { <expression> }`
  * `binding` is the binding to be defined.
  * `body` is the body of the let.
  * Example: `let x = 1 { x + 1 }`
* `Match(scrutinee: Expression, cases: List[Match.Case], …)`: `match <scrutinee> { <cases> }`
  * `scrutinee` is the value to be matched.
  * `cases` is the list of cases.
  * Example: `match #person(age: 1) { case #person(age: 1) then 1 case _ then -1 }`
* `Lambda(inputs: List[Parameter], output: Option[Type], body: Expression; …)`: `(<parameters>) -> <output> { <body> }`
  * `inputs` is the list of input parameters.
  * `output` is the output type, if given.
  * `body` is the body of the lambda.
  * Example: `(_ x: Int, _ y: Int) -> Int { x + y }`
