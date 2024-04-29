//BEGIN Print 42 should print 42 (1pt)
let main = print(42)

//OUT
42
//END

//BEGIN Print 42.5 should print 42.5 (1pt)
let main = print(42.5)

//OUT
42.5
//END

//BEGIN Printing the content of a top level let should print the value (1pt)
let x = 12
let main = print(x)

//OUT 
12
//END

//BEGIN Printing the return value of a function with no arguments returning an integer should print the integer (1pt)
fun f() -> Int {
    42
}
let main = print(f())

//OUT
42
//END

//BEGIN Printing a value in a record, indexed by index, should print the value (2pts)
let r = #record(12.5, 42)
let main = print(r.0)

//OUT
12.5
//END

//BEGIN Printing a value in a record, indexed by name, should print the value (2pts)
let r = #record(id:12.5, age:42)
let main = print(r.age)

//OUT
42
//END


//BEGIN Accessing a field of a record inside a record should work (2pts)
let r = #record(id:12.5, age:42)
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

//BEGIN If expression with condition = true should print the then branch (1pt)
let main = if true then print(42) else print(43)

//OUT
42
//END

//BEGIN If expression with condition = false should print the else branch (1pt)
let main = if false then print(42) else print(43)

//OUT
43
//END

//BEGIN If expression should evaluate the condition correctly (1pt)
let main = if ((1 + 1) == 2) then print(42) else print(43)

//OUT
42
//END

//BEGIN If expression should evaluate the branch correctly (1pt)
fun f() -> Float { 12.5 }
let main = if true then print(f()) else print(43)

//OUT
12.5
//END

//BEGIN Function call with no arguments should work (1pt)
fun f() -> Float { 42.5 }
let main = print(f())

//OUT
42.5
//END

//BEGIN Function call with arguments should work (1pt)
fun f(x: Int, y: Int) -> Int { x + y }
let main = print(f(x: 1, y: 2))

//OUT
3
//END

//BEGIN Function call with record arguments should work (1pt)
fun getAge(r: #person(id: Float, age: Int)) -> Int {r.age}
let main = print(getAge(r: #person(id: 12.5, age: 46)))

//OUT
46
//END

//BEGIN Function call with record arguments from toplevel let should work (1pt)
fun getAge(r: #person(id: Float, age: Int)) -> Int {r.age}
let p = #person(id: 12.5, age: 47)
let main = print(getAge(r: p))

//OUT
47
//END
