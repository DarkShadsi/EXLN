# EXLN Grammar & Language Reference

Source of truth for the implementation. Update this file first whenever the grammar changes.

## 1. Language rules

- Dynamically typed (`var`), static (lexical) scoping, case-sensitive
- Statements end with `;` (a block `{ }` does not take one)
- Identifiers start with a **letter**, then letters, digits, or `_`. No max length. A leading `_` is invalid.
- Keywords are reserved and cannot be used as identifiers
- Comments: `//` to end of line

## 2. Reserved words

```
and      break    case     class    continue  default  do      else
enum     false    for      func     if        null     or      print
return   super    switch   this     true      var      while
```

## 3. Full EBNF

```ebnf
(* ---------- Program & Declarations ---------- *)
program      ::= declaration* EOF
declaration  ::= classDecl | funDecl | enumDecl | varDecl | statement

(* a method uses the same rule as a function; a method named "init" is the initializer *)
classDecl    ::= "class" identifier ( "<" identifier )? "{" function* "}"
funDecl      ::= "func" function
function     ::= identifier "(" parameters? ")" block
parameters   ::= identifier ( "," identifier )*
enumDecl     ::= "enum" identifier "{" identifier ( "," identifier )* "}"
varDecl      ::= "var" identifier ( "=" expression )? ";"

(* ---------- Statements ---------- *)
statement    ::= exprStmt | forStmt | ifStmt | switchStmt | printStmt
               | returnStmt | whileStmt | doWhileStmt
               | breakStmt | continueStmt | block

exprStmt     ::= expression ";"
forStmt      ::= "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";" expression? ")" statement
ifStmt       ::= "if" "(" expression ")"
                 ( block "else" ( block | ifStmt ) | statement )
switchStmt   ::= "switch" "(" expression ")" "{"
                 caseClause* ( "default" ":" statement* )? "}"
caseClause   ::= "case" expression ":" statement*
printStmt    ::= "print" expression ";"
returnStmt   ::= "return" expression? ";"
whileStmt    ::= "while" "(" expression ")" statement
doWhileStmt  ::= "do" block "while" "(" expression ")" ";"
breakStmt    ::= "break" ";"
continueStmt ::= "continue" ";"
block        ::= "{" declaration* "}"

(* ---------- Expressions ---------- *)
expression   ::= assignment
assignment   ::= ( call "." )? identifier "=" assignment
               | call "[" expression "]" "=" assignment
               | logic_or
logic_or     ::= logic_and ( "or" logic_and )*
logic_and    ::= equality ( "and" equality )*
equality     ::= comparison ( ( "==" | "!=" ) comparison )*
comparison   ::= term ( ( ">" | ">=" | "<" | "<=" ) term )*
term         ::= factor ( ( "+" | "-" ) factor )*
factor       ::= unary ( ( "*" | "/" ) unary )*
unary        ::= ( "!" | "-" ) unary | call
call         ::= primary ( "(" arguments? ")" | "." identifier
                         | "[" expression "]" )*
arguments    ::= expression ( "," expression )*
primary      ::= "true" | "false" | "null" | "this"
               | NUMBER | STRING | identifier
               | "(" expression ")"
               | "[" arguments? "]"
               | "super" "." identifier

(* ---------- Lexical ---------- *)
identifier   ::= letter ( letter | digit | "_" )*
NUMBER       ::= digit+ ( "." digit+ )?
STRING       ::= '"' ( any character except '"' )* '"'
letter       ::= "a" … "z" | "A" … "Z"
digit        ::= "0" … "9"
```

## 4. Data types

| Type | Notes |
|---|---|
| Number | 64-bit double. Whole values print without `.0` (e.g. `42`) |
| String | Built-in literal type |
| Boolean | `true`, `false` |
| Null | `null` |
| Enum | User-defined ordinal type. Members numbered 0, 1, 2... in declaration order |
| Class instance | Compound, reference type. Fields are created by assignment (`obj.field = value`). Prints as `ClassName instance` |
| Array | Compound, reference type. `[1, 2, 3]`, zero-based index |

- No raw pointers. Objects and arrays are references: assignment copies the reference, not the data.
- Arrays are fixed-size. The size is set at creation (by a literal such as `[1, 2, 3]`, or by `array(size)`) and cannot change. Elements can be changed by index assignment, and an index outside `0` to `size - 1` is a runtime error.

## 5. Operators and type checking (runtime)

| Operation | Rule |
|---|---|
| `+` | Number + Number, or String + String. Anything else is a runtime error |
| `-` (binary), `*`, `/` | Numbers only |
| `-` (unary) | Number only |
| `<  <=  >  >=` | Numbers only |
| `==  !=` | Any types. Different types give `false`. Objects and arrays compare by reference |
| `and  or  !` | Any types, using truthiness |

**Truthiness:** only `null` and `false` are falsy. `0`, `""`, arrays, and instances are truthy.

Enum values support only `==` and `!=`. Arithmetic on an enum is a runtime error.

## 6. Built-in functions and methods

| Name | Description |
|---|---|
| `toNumber(x)` | String to Number (a runtime error unless the text looks like `12`, `-5`, or `3.14`), Boolean to Number (`false` = 0, `true` = 1), Number unchanged. Any other type is a runtime error |
| `toString(x)` | Any value to its text. Whole Numbers print without `.0`, arrays print as `[1, 2, 3]` |
| `toBoolean(x)` | Number to Boolean (`0` = `false`, otherwise `true`). Any other type is a runtime error |
| `length(arr)` | Number of elements in an array. Any other type is a runtime error |
| `array(size)` | New array with `size` slots, each `null`. `size` must be a non-negative whole Number, otherwise a runtime error |
| `Enum.values()` | Array of all members of the enum, in order |
| `member.order()` | Zero-based position of an enum member |

## 7. Control flow decisions

- `if` body needs braces **only when followed by `else`**. A plain `if` can be a single statement.
- `else` takes a block or another `if` (`else if` chains).
- `switch` has no fallthrough, so no `break` is needed. Cases compare with `==`.
- Loops: `for` (counter), `while` (pre-test), `do-while` (post-test, block body).
- `break` and `continue` affect only the innermost enclosing loop, and are errors outside a loop.
- No `goto`.

## 8. Functions

No grammar change: the `funDecl`, `function`, and `parameters` rules above already cover everything here.

- **Overloading by parameter count.** Several functions may share a name in the same scope if they take different numbers of parameters. A call picks the function whose parameter count equals the number of arguments.
  - Declaring the same name with the same parameter count in the same scope is an error.
  - A call with no matching parameter count is a runtime error ("No overload of 'name' takes N arguments.").
  - EXLN is dynamically typed, so overloads cannot differ by parameter type.
- **Parameter passing.** All arguments are passed by value. Primitives (Number, String, Boolean, null, enum) are copied. Arrays and class instances pass a copy of the reference, so a function can change the caller's object through it, but reassigning the parameter does not affect the caller. There is no `ref` keyword.
- **Return.** `return value;` exits the function with the value. Without a value, or without `return`, the result is `null`.
- **Scope.** Functions are closures and use static (lexical) scoping.

## 9. Classes

No grammar change: the `classDecl` rule above is final.

```
class Name { methods }
class Name < Superclass { methods }
```

- **Instances.** Calling the class, `Name(args)`, creates an instance. Fields are not declared. They are created by assignment, `obj.field = value`, and reading a field that was never set is a runtime error ("Undefined property 'x'."). A field with the same name as a method hides the method.
- **Methods.** Written like functions, without the `func` keyword. Inside a method, `this` refers to the instance. A method taken from an instance (`var m = obj.method;`) stays bound to that instance.
- **Initializer.** A method named `init` runs when the instance is created, and receives the arguments of `Name(args)`.
  - A class with no `init` accepts only `Name()`.
  - `init` always returns the instance, also on a bare `return;`.
  - `return value;` with a value inside `init` is an error.
- **Overloading by parameter count.** Methods, including `init`, are chosen by name and number of arguments, the same rule as functions.
  - Two methods with the same name and parameter count in one class are an error ("Method 'f' with N parameters is already defined in class 'A'.").
  - A call with no matching method is a runtime error ("No overload of 'f' takes N arguments.").
- **Inheritance.** `class B < A` makes `B` inherit every method of `A`.
  - A method in `B` with the **same name and parameter count** as one in `A` overrides it.
  - A method in `B` with the same name and a **different parameter count** is added to the inherited ones, so both are callable.
  - The superclass must be a class ("Superclass must be a class."). A class cannot inherit from itself.
- **`super`.** `super.method(args)` calls the superclass's version of a method on the current instance. It always refers to the superclass of the class where the method is written, also when the instance belongs to a deeper subclass. `super.init(args)` runs the superclass initializer.
- **Polymorphism.** A call such as `animal.speak()` runs the version of `speak` that belongs to the instance's class when the program runs.
- **Equality.** Two instances are equal only if they are the same object.
- **Enums and instances.** Enum members are not instances. They have no fields and cannot be assigned to.

## 10. Static checks (before the program runs)

These are reported by the resolver, and stop the program from running.

| Check | Message |
|---|---|
| `return` outside a function | Can't return from top-level code. |
| Local variable read in its own initializer | Can't read local variable in its own local initializer. |
| Same `var` or parameter name twice in one local scope | Already a variable with this name in this scope. |
| `break` or `continue` outside a loop | Cannot use 'break' outside of a loop. |
| `this` outside a class | Can't use 'this' outside of a class. |
| `super` outside a class | Can't use 'super' outside of a class. |
| `super` in a class with no superclass | Can't use 'super' in a class with no superclass. |
| A class that inherits from itself | A class can't inherit from itself. |
| `return` with a value inside `init` | Can't return a value from an initializer. |

Two `func` declarations or two methods with the same name are **not** reported here, because they may be overloads. The interpreter reports a clash when the parameter counts are equal.

## 11. Output format of values

| Value | Printed as |
|---|---|
| Number | `42`, `2.5` (no trailing `.0`) |
| String | the text, without quotes |
| Boolean, null | `true`, `false`, `null` |
| Array | `[1, 2, 3]`, with nested arrays and elements printed the same way |
| Enum member | `Color.GREEN` |
| Enum type | `<enum Color>` |
| Function | `<func name>` |
| Class | `<class Name>` |
| Instance | `Name instance` |

## 12. Runtime errors

Runtime errors stop the program and are reported with the line number. The common ones:

- Undefined variable or property
- Operands of the wrong type for an operator, including `+` between a String and a Number
- Division by zero
- Array index out of bounds, not a whole number, or not a number
- Calling something that is not a function, or calling with a wrong number of arguments
- An invalid argument to a built-in function
- Stack overflow from endless recursion