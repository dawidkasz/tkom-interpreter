# TKOM Interpreter

A general purpose programming language interpreter developed for the purpose of the Compilation Techniques (TKOM) course
at the Warsaw University of Technology.


## Usage
- To build the interpreter, execute the command `./gradlew build`
- Then, execute `java -jar build/libs/tkom-1.0-SNAPSHOT.jar [program_file_path]` to run the program
- To display the AST tree, add the flag `--display-ast`
- To see detailed usage instructions, use the `--help` flag

### Compilation Errors
Before executing the program, a syntax analysis will be conducted to check the code for type and semantic correctness (e.g., unreachable return). Such errors will be displayed as `CompilationError`.

### Runtime Errors
During program execution, in case of an error, the following exceptions can be expected:
- AppRuntimeException (base)
- AppNullPointerException
- AppZeroDivisionError
- AppCastError

They can be caught using the `?` operator.

## Features

- Strong static typing
- Available simple data types
    - `int`
    - `float`
    - `string`
- Available collections
    - dictionary, which can contain simple types as keys and values
- Each variable can have either a specific value or a `null` value
- `while` loop
- `foreach` loop for iterating over collections
    - dictionary - iterating over keys in the order they were added
- `if`, `else` statements
- Defining functions that return any type, including `void`
- Defining global variables
- Single-line comments, starting with `#`
- The `?` operator, which catches an exception and softens it to a null value
- Arguments to functions are passed by value for simple types and by reference for collections
- Ability to redefine variables within subsequent code blocks
- Built-in functions `void print(string)` and `string input()`

## Code Examples

### Declaring and Operating on Variables

```
int x = 3 * 3 + 2 - 1; # 10

void main() {
    int y = 8 / 5; # 1
    int z;
    z = x + y; # 11
    float a = z as float + 2.0; # 13.0
    
    string b = "Ala" + " " + "ma" + " " + "kota";

    print(a as string + " | " + b); # 13.0 | Ala ma kota
}
```

### Operations on Dictionary

```
void modifyDict(dict[string, int] mp, string key, int val) {
    mp[key] = val;
}

void main() {
    dict[string, int] mp = {"a": 1, "b": 2, "c": 3};

    modifyDict(mp, "a", 3);
    modifyDict(mp, "d", 4);

    int x = mp["a"]; # 3

    foreach (string key : mp) {
        print(mp[key] as string); # 3 2 3 4
    }
}
```

### Exception Handling with `?` Operator

```
void main() {
    dict[int, int] mp = {};

    int y = mp[1]?; # Handled AppNullPointerException, y = null
    if (y == null) {
        print("No key");
    }

    int x = ((null + 1)? / null)?;
    if (x == null) {
        print("Handled exception during operations");
    }

    int z = (10 / 0)?;
    if (z == null) {
        print("Handled zero division exception");
    }
}
```

### User Interaction

```
int readNumber() {
    print("Provide number: ");
    int n = (input() as int)?;
    if (n == null) {
        print("Invalid number format");
        return null;
    }

    return n;
}

void main() {
    int a = readNumber();
    if (a == null) {
        return null;
    }

    int b = readNumber();
    if (b == null) {
        return null;
    }

    int result = a + b;

    print(a as string + " + " + b as string + " = " + result as string);
}
```

### Recursive Calls

```
int fib(int n) {
    if (n <= 2) {
        return 1;
    }

    return fib(n-1) + fib(n-2);
}

void main() {
    string fib2 = fib(2) as string;

    print(fib2);
    print(fib(10) as string);
}
```

### Using `while` Loop

```
int loop(int x) {
    return x <= 5;
}

void main() {
    int x = 1;
    while (loop(x) && 2 == 2) {
        print(x as string);
        x = x + 1;
    }
}
```

## Grammar

```ebnf
program = {functionDefinition | variableDeclaration};

statement = ifStatement |
            whileStatement |
            foreachStatement |
            variableDeclaration |
            assignment |
            functionCall |
            returnStatement;

functionDefinition = functionReturnType identifier "(" parameters ")" statementBlock;

parameters = [parameter, {"," parameter}];

parameter = type identifier;

statementBlock = "{" {statement} "}";

ifStatement = "if" "(" expression ")" statementBlock ["else" statementBlock];

whileStatement = "while" "(" expression ")" statementBlock;

foreachStatement = "foreach" "(" simpleType identifier ":" expression ")" statementBlock;

variableDeclaration = type identifier ["=" expression] ";";

assignment = (identifier "=" expression ";") |
             (identifier "[" expression "]" "=" expression ";");

functionCall = identifier "(" arguments ")";

arguments = [expression, {"," expression}];

returnStatement = "return" expression ";";

expression = andExpression {orOperator andExpression};

andExpression = relationalExpression {andOperator relationalExpression};

relationalExpression = additiveExpression [relationalOperator additiveExpression];

additiveExpression = multiplicativeExpression {additiveOperator multiplicativeExpression};

multiplicativeExpression = nullableSingleExpression {multiplicativeOperator nullableSingleExpression};

nullableSingleExpression = negatedSingleExpression ["?"];

negatedSingleExpression = ["!" | "-"] castedExpression;

castedExpression = dictKeyExpression ["as" simpleType];

dictKeyExpression = simpleExpression ["[" expression "]"];

simpleExpression = identifier |
                   literal |
                   "(" expression ")" |
                   functionCall;

functionReturnType = type | "void";

type = simpleType | parametrizedType;

parametrizedType = collectionType "[" simpleType "," simpleType "]";

simpleType = "int" |
             "float" |
             "string";

collectionType = "dict";

orOperator = "||";

andOperator = "&&";

relationalOperator = "<" |
                   ">" |
                   "<=" |
                   ">=" |
                   "==" |
                   "!=";

additiveOperator = "+" |
                   "-";

multiplicativeOperator = "*" |
                         "/"
                         "%";

identifier = (letter | "_") {letter | digit | "_"};

literal = stringLiteral |
          intLiteral |
          floatLiteral |
          dictLiteral |
          "null";

letter = "a".."z" | "A".."Z";

digit = "0" | nonZeroDigit;

nonZeroDigit = "1" .. "9";

stringLiteral = "\"" .. "\"";

intLiteral = "0" | nonZeroDigit {digit};

floatLiteral = "0." digit {digit} |
               nonZeroDigit {digit} "." digit {digit};

dictLiteral = "{" [dictLiteralKeyValuePair {"," dictLiteralKeyValuePair }] "}";

dictLiteralKeyValuePair = expression ":" expression;
```
