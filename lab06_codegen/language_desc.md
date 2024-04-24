# Alpine

Alpine is a small functional programming language that emphaizes recusrsive programming on closed and open unions of product types (aka record types). Programs are statically type checked and can be ran by an interpreter or compiled to WebAssembly.

### Language tour

As the tradition says that the first example of a programming language should be "Hello, World!".
Let's oblige:

```
let main = print("Hello, World!")
```

The rest of this section looks at more insteresting features.

#### Records

A record is an aggregate of possibly heterogeneous data types.
In Alpine, a record also has a name.
For example, the following statement introduces a constant `x` with the value `#pair(1, 2)`, which denotes a record named `#pair` having two integer fields assigned to `1` and `2`, respectively.

```
let x = #pair(1, 2)
```

The number of fields of a record is called its _arity_.
In Alpine, a record can have any number of fields but its arity is fixed.
A record with arity 0 is called a singleton.

The fields of a record may be labeled.
One may also mix labeled and non-labeled records (see pattern matching).

```
let u = #number(42, endianness: #little)
```

The type of a record describes its shape (i.e., its name, arity, and field labels) along with the types of its fields.
For example, the type of `#number(42, endianness: #little)` is `#number(Int, endianness: #little)`.

_Note: The meaning of an expression `#x` depends on the context in which it occurs._
_In a term position, it denotes a singleton but in a type position it denotes the **type** of a singleton._

The value of a record field can be selected using either its label or its index.
For example:

```
fun scale(
  _ p: #vector2(x: Float, y: Float), by f: Float
) -> #vector2(x: Float, y: Float) {
  #vector2(x: f * p.x, y: f * p.1)
}

let main = print(scale(#vector2(x: 1.3, y: 2.1), by: 2.0))
// Prints "#vector2(x: 2.6, y: 2.2)"
```

#### Open unions

The types of Alpine form a lattice whose top and bottom are called `Any` and `Never`, respectively.
This lattice represent the subtyping relation of the language, meaning that all data types are subtype of `Any`.
This property can be used to express _open_ unions of data types.

```
fun duplicate(_ x: Any) -> #pair(Any, Any) { #pair(x, x) }
let main = print(duplicate(#unit))
// Prints "#pair(#unit, #unit)"
```

Using `Any` loses static type information trough erasure.
There are two ways to recover it.
The first is to _downcast_ a value to a narrower type.

```
let x: Any = 40
let main = print((x @! Int) + 2)
// Prints "42"
```

Note that downcasting is a dangerous operation!
It is essentially an assertion that the compiler can't guarantee.
At run-time, the operation is defined if and only if the target of the cast is indeed the type of the value being converted.
Otherwise, it crashes the program.
Safer downcasting can be expressed using pattern matching, which is the second approach to narrowing.

The compile is typically able to widen the type of an expression as necessary.
For example, calling `duplicate(42)` widens `Int` to `Any` automatically.
Nonetheless, it may be desirable to use explicit widening in some situations.

```
let x = 42 @ Any // `x` has type `Any`
```

#### Closed unions

A closed union is a finite set of types.
It is expressed using the `|` operator between type expressions:

```
let x: #a | #b = #a
```

In Alpine, closed unions can only be formed over record types with different shapes.
For example, `#a | #b` and `#a | #a(Int)` are allowed but `#a(Int) | #a(Bool)` aren't.

Intuitively, `T` is subtype of a union type `U` if it is an element of `U`.
For instance, `#a` is subtype of `#a | #b`.
Further, a union type `T` is subtype of another union type `U` if and only if all elements of `T` are contained in `U`.
For instance, `#a | #b` is subtype of `#a | #b | #c`.

Just like with `Any`, the type of a value can be widen to a closed union or narrowed to a subtype with `@` and `@!`, respectively.

```
let x = #a(40) @ #a | #a(Int)
let main = print((x @! #a(Int)).0 + 2)
// Prints "42"
```

#### Pattern matching

Pattern is an alternative to downcasting for narrowing a type.

```
fun is_anonymous(_ p: #person | #person(name: String)) -> Bool {
  match p {
    case #person then true
    case #person(name: _) then false
  }
}

let main = print(is_anonymous(#person(name: "Hannah")))
// Prints "false"
```

In the function above, `p` is used as the _scrutinee_ of a match expression with two cases.
Each of these cases is composed of a pattern and an expression.
At run-time, the first case whose pattern _matches_ the scrutinee is selected to compute the result of the entire match expression.
For example, a call to `is_anonymous(#person(name: "Hannah"))` would cause the second case to be selected, resulting in the value `false`.

A pattern can test for an exact value or for any value with of a given type.
For instance, the pattern `#person(name: _)` in matches any record of type `#person(name: String)`.
Here, `_` is called a wildcard and it can match any value of its type.
The type of a wildcard can be specified explicitly with `@`, as in `_ @ String`.

_Note: The compiler can infer that the type of the pattern is `#person(name: String)` rather than `#person(name: Any)` by looking at the type of `p`._

Matched values can be extracted with binding patterns:

```
fun name(of p: #person | #person(name: String)) -> #none | #some(String) {
  match p {
    case #person then #none
    case #person(name: let n) then #some(n)
  }
}
```

Bindings can appear anywhere in a pattern.
Hence, another way to declare the function above is:

```
fun name(of p: #person | #person(name: String)) -> #none | #some(String) {
  match p {
    case #person then #none
    case let q: #person(name: String) then #some(q.name)
  }
}
```

Because testing whether a value can be narrowed to a specific type is quite common, it can be expressed more concisely using `@?`, which returns an option of the form `#none | #some(T)`.

```
fun is_human(_ p: #person | #person(name: String) | #alien(name: String)) -> Bool {
  (p @? #alien(String)) != #none
}
```

#### Type definitions

It is possible to define custom data types.
For example:

```
type Vector2 =
  #vector2(x: Float, y: Float)
type Circle =
  #circle(origin: Vector2, radius: Float)
type Rectangle =
  #rectangle(origin: Vector2, dimension: Vector2)
```

Type definitions can be recursive:

```
type List = #empty | #list(head: Any, tail: List)
let main = print()
```
