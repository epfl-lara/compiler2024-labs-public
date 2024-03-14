# Lab 02 - Parser


In this second lab, you will implement the parser for the Alpine compiler.

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

Then take your current `alpine` project, i.e., where you implemented the interpreter, and:

* copy the `parsing/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/alpine/parsing`
* copy the `util/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/alpine/util`
* copy the `Main.scala` file from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/Main.scala`
* copy the `driver/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/main/scala/alpine/driver`
* copy the new test files by copying the `test/parsing` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/test/scala/alpine/parsing`
* copy the `test/util/` directory from this week (either from zip or repo) into your `alpine` project at this place: `src/test/scala/alpine/util`
* remove the `lib/` directory from your `alpine` project
* move the interpreter tests from `src/test/scala/alpine/evaluation` to `archive/test/evaluation`. This is because these tests rely on the typechecking phase of the compiler, that we will implement later in the semester. So we keep them here to add them back later.

Your project directory structure should look like something like this:

```console
alpine/
├── archive/
│   ├── test/
│   │   ├── evaluation/                     <----- MOVE THE INTERPRETER TESTS HERE  
│   │   │   ├── InterpreterTest.scala
├── lib/                                     <----- DELETE THIS DIRECTORY
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── alpine/
│   │   │   │   ├── driver/                  <----- COPY FROM THIS WEEK FILES
│   │   │   │   ├── evaluation/
│   │   │   │   │   ├── Interpreter.scala
│   │   │   │   ├── parsing/                 <----- COPY FROM THIS WEEK FILES
│   │   │   │   │   ├── Parser.scala
│   │   │   │   │   ├── ...
│   │   │   │   ├── util/                    <----- COPY FROM THIS WEEK FILES 
│   │   │   │   │   ├── ...
│   │   │   ├── Main.scala                   <----- COPY FROM THIS WEEK FILES (replace the current one)
├── test/
│   ├── scala/
│   │   ├── alpine/
│   │   │   ├── evaluation/                  <----- MOVED TO ARCHIVE
│   │   │   ├── parsing/                     <----- COPY FROM THIS WEEK FILES
│   │   │   │   ├── ...
│   │   │   ├── util/                        <----- COPY FROM THIS WEEK FILES
```


## Submit your work

To submit your work, go to this week assignment on Moodle and upload the following file:

* `src/main/scala/alpine/parser/Parser.scala`

## General idea of the project

Let's recall the global idea of a simple compiler's pipeline:

```
Source code -> Lexer -> Parser -> Type checking -> Assembly generation
```

The lexer generates a sequence of tokens from the source code.
The parser generates an AST from the sequence of tokens.

For example, consider the following program:

```swift
let main = exit(1)
```

The lexer generates the following sequence of tokens:

```scala
List(
  Let(0: 3), // let
  Identifier(4: 8), // main
  Eq(9: 10), // = 
  Identifier(11: 15), // exit
  LParen(15: 16), // (
  Integer(16: 17), // 1
  RParen(17: 18) // )
)
```

`Let`, `Identifier`, `Eq`, `LParen`, `Integer`, `RParen` are tokens.
The number in parentheses denote the positions in the source text from which the token has been parsed, as 0-based indices in an array of code points.

Given this token stream, the parser generates the following AST:

```scala
List(
  Binding(
    main, // identifier
    None, // type
    Some( // initializer
      Application( // function call
        Identifier(exit, hello.al: 1: 12 - 1: 16),
        List( // arguments
          Labeled(
            None,
            IntegerLiteral(1, hello.al: 1: 17 - 1: 18),
            hello.al: 1: 17 - 1: 18
          )
        ),
        hello.al: 1: 12 - 1: 19
      )
    ),
    hello.al: 1: 1 - 1: 19
  )
)
```

The AST is more expressive than the sequence of tokens as it represents the structure of the source code.

## General structure of the parser and the codebase

Parsing is decomposed into multiple functions, each of them responsible for parsing a specific part of the grammar.
For example, the method `binding` is responsible for parsing binding trees, like the one shown in the previous section.
All of these parsing functions use the following core API:

* `peek`: looks at the next token without consuming it.
  * it returns either `Some(token)` or `None` if the stream is empty (i.e. we reach an EOF).
* `take()`: consumes the next token
  * it returns either `Some(token)` or `None` if the stream is empty (i.e. we reach an EOF), and consumes a token from the stream.
* `takeIf(pred: Token => Boolean)`: takes a predicate, and consumes and returns the next token **if it satisfies the predicate**
* `take(k: Token.Kind)`: shorthand for `takeIf(_.kind == k)`
* `expect(k: Token.Kind)`: shorthand for `take(k)).getOrElse(throw  FatalError(…))`
  * i.e. it takes the next token and throws an error if it is not the expected kind of token.
* `expect(construct: String, pred: Token => Boolean)`: same as `expect` but takes a construct to include in the error message.
* `report`: reports an error while parsing.
* `snapshot`: returns the current state of the parser
* `restore` : restores the state of the parser from a backup returned by `snapshot`

Observer how these methods are used in the parts of the code that have been provided to get a sense of how they can be used.
In particular, pay attention to the way `peek` and `take` (and its variants) are used.

### New elements of the language

Throughout the lab, we will see new elements of the language.

#### Types

In _Alpine_, you can create type declarations:

```swift
type Vector2 =
  #vector2(x: Float, y: Float)
type Circle =
  #circle(origin: Vector2, radius: Float)
type Rectangle =
  #rectangle(origin: Vector2, dimension: Vector2)
```

There is also closed union types (also known as a sum type):

```swift
type OptionInt = #none | #some(Int)
```

In this case, a value of type `OptionInt` can be either a `#none` or a `#some(Int)`.

You can also define recursive types:

```swift
type List = #empty | #list(head: Any, tail: List)
```

For reference, the grammar is provided inside the [`grammar.md`](./grammar.md)/[`grammar.html`](./grammar.html) file.

## Implementation

### Some hints

The parser is written in composing functions, that each parses a part of the grammar.

For example, let's have a look at the `primaryExpression()` function. This function `peek` at the next token, and depending on its nature, calls the appropriate function to parse the corresponding producing rule of the grammar.

<div class='snippet'>

```scala
private[parsing] def primaryExpression(): Expression =
  peek match
    case Some(Token(K.Identifier, s)) =>
      identifier()
    case Some(Token(K.True, _)) =>
      booleanLiteral()
    case Some(Token(K.False, _)) =>
      booleanLiteral()
    case Some(Token(K.Integer, _)) =>
      integerLiteral()
    case Some(Token(K.Float, _)) =>
      floatLiteral()
    case Some(Token(K.String, _)) =>
      stringLiteral()
    case Some(Token(K.Label, _)) =>
      recordExpression()
    case Some(Token(K.If, _)) =>
      conditional()
    case Some(Token(K.Match, _)) =>
      mtch()
    case Some(Token(K.Let, _)) =>
      let()
    case Some(Token(K.LParen, _)) =>
      lambdaOrParenthesizedExpression()
    case Some(t) if t.kind.isOperatorPart =>
      operator()
    case _ =>
      recover(ExpectedTree("expression", emptySiteAtLastBoundary), ErrorTree.apply)
```

<p class='snippet-path'>src/main/scala/alpine/parsing/Parser.scala</p>
</div>


In general, each function parses a producing rule of the grammar (see [`grammar.md`](./grammar.md)).

An important thing to understand and be careful with is when a token is peeked and when it is taken. This is an important aspect of the parser, so think about it before starting to implement.

Some of the functions in the parser are high order parsers: this means that they take another parser as argument. For example, `commaSeparatedList` takes a parser as argument and returns a parser that parses a list of elements separated by commas.

One last interesting aspect about our pipeline is that some operators are not tokenized by the tokenizer. For example, `>=` is tokenized as two tokens `>` and `=`. This is because the `>` can be used in other contexts (s.t. `List<Int>`). Therefore, the parser is responsible from handling this case and create the operator or not depending on the context. This is an important technique in the world of compilers.

### Your Task: Implement the Parser

We suggest to start implementing by `conditional()`. 

#### Some hints about how to parse the grammar

This section contains a non-exhaustive list of hints to help you parse and understand the grammar.

##### Compound expressions `compoundExpression()`

Look at the grammar and we can see that a compound expression is primary expression followed by a '.', a '(', or nothing. Here are some examples and the corresponding type of the node produced by the parser when the primary expression is followed by '.':

* `#record(a: 1).1`: `Selection(Record(…), IntegerLiteral(1, …))`
* `#record(a: 1).a`: `Selection(Record(…), Identifier("a", …))`
* `#record(a: 1).+`: `Selection(Record(…), Identifier("+", …))`
* `a.b`: `Selection(Identifier("a", …), Identifier("b", …)`

##### Prefix expressions `prefixExpression()`

```grammar
PrefixExpression -> InfixOp | InfixOp CompoundExpression | CompoundExpression
```

A prefix expression checks if the next token is an operator. **If there is no space between the operator and the next token** (see `noWhitespaceBeforeNextToken`), parse the prefix operator and the compound expression that follows. It returns a `PrefixApplication` AST node. The fact we have to check for white space presence shows us that this gammar is not a context-free grammar.

If there is a whitespace, then it returns directly the operator (recall: it's an `Identifier`.)

In the case where it is not an operator, it will parse the compound expression (so call the `compoundExpression()` function.)

##### `ascribed()`

An ascribed expression is a prefix expression followed by an optional type cast. It returns a `AscribedExpression` AST node if there is a type cast, otherwise just a prefix expression. You can use the `typecast` function.

Example:

* `a @ Int`: `AscribedExpression(Identifier("a", …), Typecast.Widen, TypeIdentifier("Int", _), _)`
* `1 @ Int`: `AscribedExpression(IntegerLiteral(1, …), Typecast.Widen, TypeIdentifier("Int", _), _)`
* `1`: `IntegerLiteral(1, …)` (returned by the `prefixExpression` function)

##### `infixExpression` and `expression()`

As we saw in the lecture, parsing expressions requires care because of the ambiguity introduced by precedence.

Notice that infixEpression takes a precendence as input: you may use it this parameter to factor out the parsing of all possible infix expressions with different precedence levels.

You may take inspiration from the precedence climbing algorithm to parse infix expressions. See [the Wikipedia article](https://en.wikipedia.org/wiki/Operator-precedence_parser#Precedence_climbing_method) for more information.

##### Literals

To get the text a token contain, you can use the `.site.text` method.

##### `labeled(…)`

A `Labeled[T]` is a value of type `T` (as in Scala) with an optional `String` denoting its label.

In _Alpine_, a `Labeled[T]` can be:

* `<value>`
* `<label>: <value>`

_Hint_: you may find the `snapshot` and `restore` methods useful.

_Note_: as stated in the grammar, a `<label>` can be an `<identifier>` or a `<keyword>`.

_Examples_:

* `label: 1` → `Labeled(Some("label"), IntegerLiteral(1, …))`
* `match: 1` → `Labeled(Some("match"), IntegerLiteral(1, …))`
* `1` → `Labeled(None, IntegerLiteral(1, …))`

##### `inParentheses`, `inBraces`, `inAngles`

Complete the three different functions that parses an `element` delimited by parentheses (`(<element>)`), braces (`{<element>}`) and angles (`<>`)

##### `parenthesizedLabeledList(value: () => T): List[Labeled[T]]`

This function parses a list of labeled values delimited by parentheses. The list can be empty.

For example, the above function is responsible to parse the following code:

```swift
(1, 2, label: 3)
```

_Hint_: `inParentheses`, `labeled` and `commaSeparatedList` are useful to implement this function.

##### Records

In this part, we will break down the record parsing. You should implement `recordExpression()`, `recordExpressionFields()` and `record(fields: () => List[Field], make: (String, List[Field], …) => T)`

* The `record` function is responsible for parsing a record. It returns a `T` AST node. In the case of parsing record expressions, `T` is `ast.Record`. It is general and will be used as well for `recordType`s

* `recordExpression()` is a function used to parse a record expression. It returns a `Record` AST node.

* The `recordExpressionFields()` function is responsible for parsing the fields of a record expression. It returns a `List[Labeled[Expression]]` AST node.

<div class="hint">

Do forget that you can reuse parser functions you already implemented so far.

</div>

It should parse the following sub grammar:

```
Record -> '#' Identifier  ['(' LabeledExpressionList ')']
```

<div class="note">

An identifier with a `#` prefix is a special token called `Label`.

Note as well that `Field` in `record(…)` is a generic type! It should be of subtype `Labeled[Tree]` and `Labeled` is covariant. It will come handy when we will parse record types.

</div>

##### Conditionals `conditional()`

```if <expression> then <expression> else <expression>```

which correspond to:

```IfExpression -> 'if' Expression 'then' Expression 'else' Expression``` in the grammar.

The `if` function has necessarily to have an `else` branch. The `else` branch is mandatory in _Alpine_.

##### `tpe()`

The `tpe` function is responsible for parsing a type. It returns a `Type` AST node. For the fact, `tpe` is called `tpe` because `type` is a reserved keyword in Scala.

`tpe` should parse the `Type` given the grammar above. You can call the `primaryType` function to parse a `PrimaryType`.

##### `recordType()` and `recordTypeFields()`

Now, implement the `recordType` and `recordTypeFields` functions. They are responsible for parsing a record type and its fields. They should return a `RecordType` and a `List[Labeled[Type]]` respectively.

In the same manner as `recordExpression` and `recordExpressionFields`, `recordType` should call the `record` function.

##### `arrowOrParenthesizedType()`

When encountering a `(` token, it can be either a function's type or a parenthesized type (a parenthesized type is a type between parentheses). This function should parse the two cases and return the corresponding AST node.

It should parse both cases:

* Parenthesized type (`ParenthesizedType`):

```
(<type>)
```

and

* Arrow/Lambda type:

```
(<type1>, <type2>, …, <typeN>) -> <type>
```

##### Bindings & let

###### `binding()`

A binding is a top-level declaration that binds an identifier to a value. It has the following form:

```
Binding -> 'let' Identifier [':' Type] ['=' Expression]
```

where `[':' Type]` and `['=' Expression]` is not always optional: the argument `initializerIsExpected` is `true` if the initializer is expected (i.e. not optional) and `false` otherwise (i.e. optional): it may come handy for later! Implement the `binding()` function that parses a binding.

##### Functions: `function()`, `valueParameterList()`, `parameter()`

##### `parameter()`

A parameter is of the form:

```
<identifier> <identifier> [: <type>] // labeled
'_' <identifier> [: <type>] // unlabeled
<keyword> <identifier> [: <type>] // labeled by keyword
```

In the first case, the first element is the label of the parameter (can be an identifier, `_` or a keyword) and the second element is the name of the argument. When labeled by a keyword, the keyword is the label. When labeled, the parameter's name (i.e. its identifier inside the function) is the second identifier. When unlabeled (i.e. `_` is the label), the parameter's name is the first and only identifier.

Implement the `parameter` function that parses a parameter. It returns a `Parameter` AST node.

_Note_: here the label is before the identifier without any separator token. It is not the case for `labeled` where the label is separated by a colon with the identifier.

* Examples: 
  * `_ x: Int`: `Parameter(None, "x", Some(TypeIdentifier("Int", _)), _)`
  * `label x: Int`: `Parameter(Some("label"), "x", Some(TypeIdentifier("Int", _)), _)`
  * `label x`: `Parameter(Some("label"), "x", None, _)`

###### `valueParameterList()`

A value parameter list is a list of parameters. It has the following form:

```
( <parameter1>, … )
```

Implement the `valueParameterList` function that parses a value parameter list. It returns a `List[Parameter]` AST node.

_Hint_: you may find the `commaSeparatedList` function and `parameter()` useful.

##### `function()`

A function is of the form:

```
fun <identifier> (<type parameters>) [-> <type>] { <expression> }
```

where `[-> <type>]` is optional. Implement the `function` function that parses a function. It returns a `Function` AST node. 

### `lambdaOrParenthesizedExpression()`

When encountering a `(` token, it can be either a lambda or a parenthesized expression. This function should parse the two cases and return the corresponding AST node.

It should parse both cases:

```
(<expression>)
```

and

```
(<value parameter list>) [-> type] { <expression> }
```

where `[-> type]` is optional.

_Hint_: `snapshot` and `restore` may come handy.

##### Match expressions

A match expression is a conditional expression that matches a value against a set of patterns. It has the following form:

```swift
match <expression> {
  case <pattern> then <expression>
}
```

Let's decompose this pattern into smaller parts as we did for the other elements of the language.

###### `mtch()`

The `mtch` function is responsible for parsing a match expression. It returns a `Match` AST node. It expects a `match` token, an expression and then calls `matchBody` to parse the body of the match expression.

###### `pattern()` with `wildcard()`, `recordPattern()`, `bindingPattern()` and `valuePattern()`

The four functions are responsible for parsing a pattern. They return a `Pattern` AST node.

* `wildcard` should parse the `_` token.
* `valuePattern` should parse an `expression` and return a `ValuePattern` AST node.
* `bindingPattern` should parse a `binding()` without an initializer!
* `recordPattern` should call the `record` function and return a `RecordPattern` AST node. Don't forget that you've made a function to parse records! However, it is required to fill the `recordPatternFields()` function.
