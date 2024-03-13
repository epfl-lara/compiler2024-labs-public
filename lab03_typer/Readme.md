# Lab 03 - Typer

In this third, you will implement the typer for the Alpine compiler.

## Obtaining the lab files

To get the lab files, you have 2 options:

* pull this repository if you already cloned it last week. Otherwise, you can clone it by running the following command:

  ```console
  $ git pull
  ```

  or

  ```console
  $ git clone https://github.com/epfl-lara/compiler2024-labs-public.git
  ```

* Download the zip file on Moodle

Then take your current `alpine` project, i.e., where you implemented the interpreter and the parser, and:

* copy the `typing/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/alpine/typing`
* copy the `Main.scala` file from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/Main.scala`
* copy the `driver/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/alpine/driver`
* copy the new test files by copying the `test/typing` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/test/scala/alpine/typing`
* move the interpreter tests from `archive/test/evaluation` back to`src/test/scala/alpine/evaluation`.

Your project directory structure should look like something like this:

```console
alpine/
├── archive/                                 <----- YOU CAN DELETE THIS DIRECTORY
│   ├── test/
│   │   ├── evaluation/                    
│   │   │   ├── InterpreterTest.scala
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── alpine/
│   │   │   │   ├── driver/                  <----- COPY FROM THIS WEEK FILES (replace the current one)
│   │   │   │   ├── evaluation/
│   │   │   │   │   ├── Interpreter.scala
│   │   │   │   ├── parsing/                 
│   │   │   │   │   ├── Parser.scala
│   │   │   │   │   ├── ...
│   │   │   │   ├── typing/                   <----- COPY FROM THIS WEEK FILES
│   │   │   │   │   ├── Typer.scala
│   │   │   │   │   ├── ...
│   │   │   │   ├── util/                    
│   │   │   │   │   ├── ...
│   │   │   ├── Main.scala                   <----- COPY FROM THIS WEEK FILES (replace the current one)
├── test/
│   ├── scala/
│   │   ├── alpine/
│   │   │   ├── evaluation/                  <----- MOVE BACK FROM ARCHIVE
│   │   │   │   ├── InterpreterTest.scala
│   │   │   ├── parsing/                     
│   │   │   │   ├── ...
│   │   │   ├── typing/                       <----- COPY FROM THIS WEEK FILES
│   │   │   │   ├── TyperTest.scala
│   │   │   ├── util/                        
```


## Submit your work

To submit your work, go to this week assignment on Moodle and upload the following file:

* `src/main/scala/alpine/typing/Typer.scala`

## General idea of the project

Let's recall the global idea of a simple compiler's pipeline:

```
Source code -> Lexer -> Parser -> Type checking -> Assembly generation
```

During this lab, you will implement the type checking phase of the Alpine compiler. Note that it does three things:

* It checks that the program is well-typed, i.e. that the types of the expressions are consistent with the types of the variables, the types of the functions (including the fact that the function is called with the right number of arguments and that the types of the arguments are consistent with the types of the parameters), and the types of the constants.
* It also infers the types of expressions that do not have a type ascription, i.e. it computes the type of each expression and stores it in the AST.
* It also perform “name resolution”: every variable and function call should be resolved to a unique definition (this is the `referredEntity` field from the interpreter lab.) For example, if the program contains two function definitions `fun f(x: Int) -> Int { ... }` and `fun f(x: Float) -> Float { ... }`, then the call `f(42)` should be resolved to one of the two definitions. The choice of the definition depends on the type of the argument `42`.

### Supported language features

The type checker supports all the features described in the language description (i.e., in this [file]("language_desc.md")), EXCEPT the ***Type declarations***. This means, your typer does not have to handle the constructs of the form

```swift
type T = Int
type ListInt = #nil | #cons(head: Int, tail: ListInt)
```

Specifically, the subset of the language that the typer will handle does NOT include recursive types.

### Basics

The typer will be implemented in the `Typer.scala` file. It will visit the AST and return the computed/discovered types after having checked their consistence.

#### Constraints

To solve the typing problem, we will generate a set of constraints and solve them using the provided `Solver`.

Here are the available constraints that the solver can handle:

* `Equal(lhs: Type, rhs: Type, origin: Origin)`: the left hand side and the right hand side should have the same type.
* `Subtype(lhs: Type, rhs: Type, origin: Origin)`: the left hand side should be a subtype of the right hand side.
* `Apply(function: Type, inputs: List[Type.Labeled], output: Type, origin: Origin)`: the function should be a function type with the given inputs and output types.
* `Member(lhs: Type, rhs: Type, member: String | Int, selection: Expression, origin: Origin)`: the left hand side should have a member with the type `rhs`, i.e., something of the `lhs.member: rhs`. `selection` is the expression of the member selection.
* `Overload(name: Tree, candidates: List[EntityReference], tpe: Type, origin: Origin)`: this constraint is responsible of the name analysis mentioned above. The name should be the name of entity (e.g., a function names) that can refer to multiple entities. All those entities are passed in  `candidates`. This constraint indicates to the solver that one of the entity in the list should have the type `tpe`. The `Solver` will then pick the right entity if it exists. This is how the `Typer` will resolve the ambiguity of the function call `f(42)` mentioned above.
  
The origin is the AST node that generated this constraint (this is to show a meaningful error to the user in case the constraint cannot be solved).

All of those constraints are stored in an instance of the `ProofObligations` class that will be passed to the solver.

To add new constraints you can use:

* `context.obligations.constrain(e, type)` to add an equality constraint that the expression `e` should have type `type`. This function also returns the currently inferred type of `e`.
* `context.obligations.add(constraint)` to add a constraint `constraint` to the obligations (any type of constraint, unlike the other possible function).

#### Fresh type variables

When infering types, it is not always possible to know the type of an expression when encountering it. For example, let us take the following code:

```swift
let a = 1
let x = if (a <= 0) { #person(name: "Jean") } else { #person(name: "Jean", age: a) }
```

In this case, when typing the `if` expression, we cannot fix the type of the `then` and `else` branches. This is because we do not know the type of `x` (we are inferring it), and we only know that the type of an `if` expression is a *supertype* of the types of the `then` and `else` branches.

So to infer the type of `x`, we assign it a *fresh type variable* $\tau$, and we add constraint for this type. The `Solver` is then responsible to find a type for $\tau$ that satisfies all the constraints.

To generate a fresh type variable, please use the `freshTypeVariable()` method.

#### Visitor

We have provided the implementations of the `visit` function of identifiers and literals, as well as for the pattern matching. You will have to implement the `visit` functions for the other AST nodes.

```scala
def visitBooleanLiteral(e: ast.BooleanLiteral)(using context: Typer.Context): Type =
  context.obligations.constrain(e, Type.Bool)

def visitIntegerLiteral(e: ast.IntegerLiteral)(using context: Typer.Context): Type =
  context.obligations.constrain(e, Type.Int)

def visitFloatLiteral(e: ast.FloatLiteral)(using context: Typer.Context): Type =
  context.obligations.constrain(e, Type.Float)

def visitStringLiteral(e: ast.StringLiteral)(using context: Typer.Context): Type =
  context.obligations.constrain(e, Type.String)
```

In each of these methods, we add a new constraint that the visited AST node is of type `Type.Bool`, `Type.Int`, `Type.Float` or `Type.String` respectively.

```scala
def visitIdentifier(e: ast.Identifier)(using context: Typer.Context): Type =
  bindEntityReference(e, resolveUnqualifiedTermIdentifier(e.value, e.site))
```

In this method, we make calls to:

* `resolveUnqualifiedTermIdentifier` to resolve the identifier to a list of possible entities. This step will find all the entities that the identifier can refer to. For example, if the identifier is `f`, it will return all the functions that are named `f`.
* `bindEntityReference(e: ast.Tree, candidates: List[EntityReference])` to bind the AST node to the resolved entities
  * if there is no candidates, then the type of `e` is `Type.Error`
  * if there is a single candidate, then the type of `e` is the type of the candidate. Moreover, it links the AST node to the candidate using the `properties.treeToReferredEntity` map.
  * if there is more than a single candidate, then a new fresh type variable $\tau$ is created and the AST node is linked to the type variable $\tau$. Moreover, a new `Overload` constraint is added to the obligations to say that the type of `e` is the type of one of the candidates. Also, a constraint is added to say that the type of `e` is $\tau$.

Note that there is some useful functions in the `Typer` class that you can use to generate fresh type variables, get the type of an entity, check if a type is a subtype of another, etc.

### Scope

In programming, the *Scope* of a binding is defined as follows: 

> [The ] scope is "the portion of source code in which a binding of a name with an entity applies".

For example, in the following code:

```swift
fun f(x: Int) -> Int {
  let y = 42 {
    x + y
  }
}
let main = let z = 1 {
  f(z)
}
```

the scope of the binding of `y` is  the body of the `let` binding, so:

```swift
{
  x + y
}
```

and the scope ot the binding of `x` is the body of the function `f`, so:

```swift
{
  let y = 42 {
    x + y
  }
}
```

### Running the type checker

Inside SBT, you can use the `run -t <input file>` command to run the type checker on a file. You can also add the `--type-inference` flag to show the solving part of the type checker (so including the constraints and the solution).

### How is overloading solved?

Let us recall that overloading is the ability to have multiple functions with the same name but different types. For example, in the following code:

```swift
fun f(x: Int) -> Int { x }
fun f(x: Float) -> Float { x }
```

the function `f` is overloaded. When the type checker encounters a call to `f`, it should resolve the call to one of the two functions. This is what the `Overload` constraint is for.

Overloading is resolved by the `Solver` class. For every `Overload` constraint and their candidates, the solver forks itself and tries to solve the other constraints with each candidate (i.e., type checking the program assuming the candidate is the right one). If one of the forks fails, the solver backtracks and tries with the next candidate. If all the forks fail, the solver reports an error. And if one of the forks succeeds, the solver returns the solution. 

If you run the compiler with the `--trace-inference` flag with a piece of code that contains some overload, you will see that the solver tries to solve the constraints with each candidate.

### Memoization

You can see some calls to `memoizedUncheckedType`. Its role is to store the type of a tree node in an *unchecked* map. This means the type is temporary, but not checked yet.

This is used for recursive functions: the type of the function is stored in this map, so that the body can be typechecked while "knowing" the type of the function itself (for possible recursive calls). This is also used for recursive types, but we do not handle them in this lab.

This is also used for memoization, i.e., not recomputing the type of a tree multiple times.

### Hints and inference rules

Below are listed hints about the visit functions you have to implement, along with the inference rules of the alpine type system.

#### `visitRecord(e: ast.Record)`

$$\frac{
  \Gamma \vdash e_1 : \tau_1 \quad \Gamma e_2 : \tau_  \Gamma \quad \dots \quad \Gamma e_n \quad \Gamma: \tau_n
}{
  \char"23 a(a_1: e_1, a_2: e_2, \dots, a_n: e_n): \char"23 a(a_1: \tau_1, a_2: \tau_2, \dots, a_n :\tau_n)
}$$

where $\char"23 a$ is the record name and $a_1$, …, $a_n$ are the labels.

For each field of the record, you should visit recursively the expression of the field to get field types.

_Note_: Math mode can be buggy on GitHub Markdown if the above rule is not properly displayed. You can download the .html file and open it in your browser to see it properly! Or open the Markdown file inside your editor. All `\char"23` should be displayed as `#`.

#### `visitApplication(e: ast.Application)`

Two constraints should be addeed:

* `Apply`: the application is valid.
* `e`: should have the type $\tau$

$$
\frac{
  \Gamma \vdash f: ((l_1: \tau_1, l_2: \tau_2, \dots, l_n: \tau_n) \to \tau) \quad \Gamma e_1: \tau_1 \quad  \Gamma e_2: \tau_2 \quad \dots \quad \Gamma e_n: \tau_n
}{
  \Gamma \vdash f(l_1: e_1, l_2: e_2, \dots, l_n: e_n): \tau
}
$$

This means that if $f$ has type $(l_1: \tau_1, l_2: \tau_2, \dots, l_n: \tau_n) \to \tau$, and $e_1$ has type $\tau_1$, $e_2$ has type $\tau_2$, etc., then the application has type $\tau$.

You should get the type of the function and of the labeled arguments. If the output type of the function is not known, you should generate a fresh type variable for it. Otherwise, $\tau$ is the output type of the function.

#### `visitPrefixApplication` / `visitInfixApplication`

The idea is simlar to the `visitApplication` method. Keep in mind that infix and prefix applications do not have labels and their number of arguments is fixed (1 for prefix and 2 for infix).

Prefix applications:

$$
\frac{
  \Gamma \vdash f: \tau_1 \to \tau  \quad \Gamma e: \tau_1
}{
  \Gamma \vdash f(e): \tau
}
$$

Infix applications:

$$
\frac{
  \Gamma \vdash f: (\tau_1, \tau_2) \to \tau, e_1: \tau_1, e_2: \tau_2
}{
  \Gamma \vdash f(e_1, e_2): \tau
}
$$

#### `visitConditional(e: ast.Conditional)`

<div class="hint">

Check the `checkInstanceOf` function.

</div>

The type of a conditional is $\tau$ which is a supertype of the type of the `then` branch and the type of the `else` branch. The condition must be a `Boolean`. Generate fresh variables if needed.

$$
\frac{
  \Gamma \vdash e_1: \text{Boolean }, e_2: \tau_2, e_3: \tau_3, \tau >: \tau_2, \tau >: \tau_3
}{
  \Gamma \vdash \text{if } e_1 \text{ then } e_2 \text{ else } e_3: \tau
}
$$

#### `visitLet(e: ast.Let)`


$$
\frac{
  \Gamma \vdash e: \tau, \,\,\Gamma,x \mapsto e \vdash e_r: \tau_r
}{
  \Gamma \vdash \text{let } x: \tau = e \,\{ e_r \}: \tau_r
}
$$

You must assign a new scope name using the `assignScopeName` method, and visit the `binding` and the body with this new scope name.

#### `visitLambda(e: ast.Lambda)`

You should assign a new scope name for that lambda and in that scope, you should get the type of the inputs (using `computedUncheckedInputTypes`)

In the same manner as the application, get the output type of the lambda or generate a fresh variable, and add the following constraints:

* `e` should be of type `Type.Arrow`.
* the body should be a subtype of the output type of the lambda.

$$
\frac{
  \Gamma x_1: \tau_1, \ldots, x_n: \tau_n \vdash e <: \tau
}{
  \Gamma : (l_1: x_1: \tau_1, \ldots, l_n: x_n: \tau_n) \,{\color{green} \to \tau} \,\{ e \} : ((l_1: \tau_1, \ldots, l_n: \tau_n) \to \tau)
}
$$

where the first ${\color{green} \to \tau}$ is optional.

#### `visitParenthesizedExpression`

$$
\frac{\Gamma \vdash e : \tau}{\Gamma \vdash (e): \tau}
$$

#### `evaluateTypeTree`

When encoutering a specified type, you should call `evaluateTypeTree`.

#### `visitAscribedExpression(e: ast.AscribedExpression)`

You should evaluate the `ascription` field of the `AscribedExpression` using `evaluateTypeTree` and depending on the ascription. Then add needed constraints, depending on the ascription sort:

<!-- * if it's a widening ascription, the expression type should be a subtype of the type given in the ascription.
* if it's a narrowing unconditionally ascription, use `checkedTypeEnsuringConvertible` to check that the type of the expression is a subtype of the type given in the ascription, and return the type given in the ascription.
* if it's a narrowing ascription, use `checkedTypeEnsuringConvertible` to check that the type of the expression is a subtype of the type given in the ascription, and return the optional type that corresponds to it. -->

$$
\frac{\Gamma \vdash e <: \tau}{\Gamma \vdash e \, @ \,\tau : \tau}
$$
$$
\frac{\Gamma \vdash e :> \tau}{\Gamma \vdash e \, @! \,\tau : \tau} \text{ while ensuring convertibility}
$$
$$
\frac{\Gamma \vdash}{\Gamma \vdash e \, @? \,\tau : \text{Option}[\tau]}
$$

#### Meta-types

Metatypes are the types of types. For example, in an alpine program, the type `Int` in `let x : Int = 42 { ... }` has the type `MetaType[Int]`. This is useful to type check an expression where a type is expected, for example in a type ascription or arrow types.

#### `visitTypeIdentifier(e: ast.TypeIdentifier)`

You should lookup the type identifier in the current scope:

1. if there is no type with that name, return `Type.Error` and report an error.
2. if there is a single type with that name, return the meta-type corresponding to the type (`Type.Meta`).
3. if there is more than a single type with that name, return a `Type.Error` and report an ambiguous use of the type.

<div class="hint">

Check the provided functions.

</div>

#### `visitRecordType`

You should return the type of the record type, which is a `Type.Record` with proper fields.

$$
\frac{
  \Gamma \vdash \tau_1: \text{Meta}[\text{Labeled}] \quad \ldots \quad \Gamma \tau_n: \text{Meta}[\text{Labeled}] \quad \Gamma\tau_r: \text{Meta}[\tau_r]
}{
  \Gamma \vdash \char"23 a(\tau_1, \ldots, \tau_n) \to \tau_r
}
$$

Note that $\tau_1$, …, $\tau_n$ are labelled types.

#### `visitArrow`

You should return the type of the arrow type, which is a `Type.Arrow` with proper inputs and output.

$$
\frac{
  \Gamma \vdash \tau_1: \text{Meta}[\text{Labeled}] \quad \ldots \quad \Gamma \tau_n: \text{Meta}[\text{Labeled}] \quad \Gamma\tau_r: \text{Meta}[\tau_r]
}{
  \Gamma \vdash (\tau_1, \ldots, \tau_n) \to \tau_r
}
$$

Note that $\tau_1$, …, $\tau_n$ are labelled types.

#### `visitParenthesizedType`

$$
\frac{
  \Gamma \vdash \tau: \text{Meta}[\tau_{in}]
}{
  \Gamma \vdash (\tau) \to \text{Meta}[\tau_{in}]
}
$$

#### `visitRecordPattern`

On the same principle as the `visitRecord` method and `visitValuePattern`, you should add a constraint for the type of the pattern.

$$
\frac{\Gamma \vdash}{
  \Gamma \vdash \char"23 a(l_1: p_1, \ldots, l_n: p_n): \char"23 a(l_1: \tau_1, \ldots, l_n: \tau_n)
}
$$

where $\char"23 a$ is the record name and $l_1$, …, $l_n$ are the labels.

#### `visitWildcard`

$$
\frac{\Gamma \vdash}{
  \Gamma \vdash \_ : \tau
}
$$

#### `visitFunction`

$$
\frac{
  \Gamma, x_1: \tau_1, \ldots, x_n: \tau_n \vdash e <: \tau
}{
  \Gamma \vdash (\text{fun } f(x_1: \tau_1, \ldots, x_n: \tau_n) \to \tau \{ e \}) : (l_1: x_1: \tau_1, \ldots, l_n: x_n: \tau_n) \to \tau
}
$$

You should get the unchecked type of the function and memoize it. You should then type check the body and it must be a subtype of the output type of the function.

#### `visitSum`

The function is provided, but here is the inference rule:

$$
\frac{
  \Gamma \vdash e: \tau_i
}{
  \Gamma \vdash e: \tau_1 | \tau_2 | \ldots | \tau_i | \dots | \tau_n
}
$$

#### `visitSelection`

A selection can be performed on a record with an identifier or an integer. You should add a `Member` constraint.

For the integer selector:

$$
\frac{
  \Gamma \vdash e: \char"23 a(l_1: \tau_1, \dots, l_n:\tau_n)
}{
  \Gamma \vdash e.i: \tau_i
}
$$

For the identifier selector:

$$
\frac{
  \Gamma \vdash e: \char"23 a(l_1: \tau_1, \dots, l_n:\tau_n)
}{
  \Gamma \vdash e.l_i: \tau_i
}
$$