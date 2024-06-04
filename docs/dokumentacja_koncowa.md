# TKOM - Dokumentacja końcowa
Dawid Kaszyński

## Korzystanie z interpretera
- W celu zbudowania interpretera należy wykonać komendę `./gradlew build`
- Następnie wykonaj polecenie `java -jar build/libs/tkom-1.0-SNAPSHOT.jar [program_file_path]` w celu wykonania programu
- W celu wyświetlenia drzewa AST dodaj flagę `--display-ast`
- Aby zobaczyć dokładny sposób użycia, wykorzystaj flage `--help`

### Błędy kompilacji
Przed rozpoczęciem wykonania programu zostanie przeprowadzona analiza składniowa sprawdzająca kod
pod kątem zgodności typów oraz semantyki (np. nieosiągalny return). Błędy tego typu zostaną wyświetlone jako `CompilationError`.

### Błędy czasu wykonania
Podczas wykonania programu w przypadku błędu można spodziewać się następujących wyjątków:
- AppRuntimeException (bazowy)
- AppNullPointerException
- AppZeroDivisionError
- AppCastError

Można je przechwycić z wykorzystaniem operatora `?`.

## Charakterystyka

- Typowanie silne statyczne
- Dostępne proste typy danych
  - `int`
  - `float`
  - `string`
- Dostępne kolekcje
  - słownik, który jako klucze i wartości może zawierać typy proste
- Każda zmienna może mieć albo konkretną wartość, albo wartość `null`
- Pętla `while`
- Pętla `foreach` umożliwiająca iterację po kolekcjach
  - słownik - iterowanie po kluczach w kolejności takiej w jakiej zostały w nim umieszczone
- Instrukcje `if`, `else`
- Definiowanie funkcji zwracających dowolny typ, w tym `void`
- Definiowanie zmiennych globalnych
- Komentarze jednolinijkowe, rozpoczynane znakiem `#`
- Istnieje operator `?`, który powoduje przechwycenie wyjątku i zmiękczenie go do wartości null.
- Argumenty do funkcji przekazywane przez wartość dla typów prostych i przez referencję dla kolekcji
- Możliwość redefiniowania zmiennych w ramach kolejnych bloków kodu
- Wbudowana funkcje `void print(string)` oraz `string input()`


## Przykłady użycia

### Deklarowanie i operacje na zmiennych

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

### Operacje na słowniku
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

### Obsługa wyjątków z operatorem `?`
```
void main() {
    dict[int, int] mp = {};

    int y = mp[1]?; # Obsłużone AppNullPointerException, y = null
    if (y == null) {
        print("Brak klucza");
    }

    int x = ((null + 1)? / null)?;
    if (x == null) {
        print("Obsłużony wyjątek przy działaniach");
    }

    int z = (10 / 0)?;
    if (z == null) {
        print("Obsłużony wyjątek dzielenia przez zero");
    }
}
```

### Interakcja z użytkownikiem
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

### Wywołania rekurencyjne
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

### Wykorzystanie pętli `while`
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

## Gramatyka

```
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

## Opis realizacji i testowanie
- Implementacja interpretera w języku Java
- Środowisko do budowania gradle
- 262 testy jednostkowe bądź integracyjne