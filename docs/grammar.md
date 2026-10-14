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
| Class instance | Compound, reference type |
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
| `toNumber(x)` | String to Number (error if invalid), Boolean to Number (`false` = 0, `true` = 1) |
| `toString(x)` | Number to String |
| `toBoolean(x)` | Number to Boolean (`0` = `false`, otherwise `true`) |
| `length(arr)` | Number of elements in an array |
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

## 8. Still to define

- Class declaration details (the current class and function rules are a Lox-based proposal)
- Further built-ins