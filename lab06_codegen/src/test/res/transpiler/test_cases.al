//BEGIN Hello World should print Hello World (1pt)
let main = print("Hello World!")

//OUT
Hello World!
//END

//BEGIN Printing the content of a top level let should print the value (1pt)
let x = "koala"
let main = print(x)

//OUT 
koala
//END

//BEGIN Printing the return value of a function with no arguments returning a string should print the string (1pt)
fun f() -> String {
    "koala42"
}
let main = print(f())

//OUT
koala42
//END

//BEGIN Printing the return value of a function with no arguments returning an integer should print the integer (1pt)
fun f() -> Int {
    42
}
let main = print(f())

//OUT
42
//END

//BEGIN Printing a value in a record, indexed by index, should print the value (2pt)
let r = #record("koala", 42)
let main = print(r.0)

//OUT
koala
//END

//BEGIN Printing a value in a record, indexed by name, should print the value (2pt)
let r = #record(name:"koala", age:42)
let main = print(r.age)

//OUT
42
//END


//BEGIN Accessing a field of a record inside a record should work
let r = #record(name:"koala", age:42)
let r2 = #record(r)
let r3 = r2.0
let main = print(r3.age)
//OUT
42
//END

//BEGIN Multiple records can be accessed (2pts)
let record1 = #record(foo: 42)
let record2 = #record(foo: 43)
let main = print(record1.foo + record2.foo)

//OUT
85
//END

//BEGIN Records fields are evaluated (2pts)
let record = #record(foo: (1 + 10))
let main = print(record.foo)

//OUT
11
//END

//BEGIN Narrowing conditionally should return a #none if not a subtype (2pts)
let main = match ((5 @ Any) @? Float) {
    case #some(let x) then print(x)
    case #none then print("none")
}
//OUT
none
//END

//BEGIN Narrowing conditionally should return a #some if a subtype (2pts)
let main = match ((1 @ Any) @? Int) {
    case #some(let x) then print(x)
    case #none then print("none")
}
//OUT
1
//END

//BEGIN Narrowing unconditionally should work with valid cast (2pts)
let x = ((1 @ Any) @! Int)
let main = print(x)
//OUT
1
//END

//BEGIN Narrowing unconditionally should panic with unvalid cast (2pts)
let x = ((1 @ Any) @! Float)
let main = print(x)
//OUT
panic
//END

//BEGIN Pattern matching with values (2pts)
let status = match #a(1) {
    case (#a(2)) then 2
    case (#a(1)) then 1
    case _ then 0
}
let main = print(status)
//OUT
1
//END

//BEGIN Pattern matching with wildcard (2pts)
let status = match #a(1) {
  case #a(_) then 1
  case _ then 0
}
let main = print(status)
//OUT
1
//END

//BEGIN Pattern match on an identifier (1pt)
let x = 3
let record = #record(x)
let status = match record {
  // The following should match with the x defined above.
  case #record(x) then print("Congrats")
  case _ then print("Sorry :(")
}
let main = status
//OUT
Congrats
//END

//BEGIN Pattern matching with unconstrained binding (2pts)
let status = match #a(1) {
  case let x: #a(Int) then x.0
  case _ then 0
}
let main = print(status)
//OUT
1
//END

//BEGIN Pattern matching with constrained binding (2pts)
let status = match (#a(1) @ Any) {
  case let x: #a(Int) then x.0
  case _ then 0
}
let main = print(status)
//OUT
1
//END

//BEGIN Nested pattern matching (2pts)
let x = #x(#a(1), 2)
let status = match x {
    case #x(#a(let y), 2) then y
}
let main = print(status)
//OUT
1
//END

//BEGIN Nested pattern matching and multiple lets (2pts)
let x = #x(#a(1), 2)
let status = match x {
    case #x(#a(let y), let z) then y + z + 1 * 2
}
let main = print(status)
//OUT
5
//END