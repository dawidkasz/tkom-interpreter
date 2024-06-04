package executor;

import ast.Program;
import lexer.DefaultLexer;
import lexer.characterprovider.StringCharacterProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import parser.DefaultParser;
import parser.Parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class SemanticCheckerTest {
    private final SemanticChecker semanticChecker = new DefaultSemanticChecker();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void should_not_fail_for_a_valid_program() {
        // given
        String program = """
                int f(string a, int b, float c, dict[int, float] d) {
                    if (a == "x") {
                        return 1;
                    }
                    if (b == 1) {
                        return 2;
                    }
                    if (c == 0.0) {
                        return 3;
                    }

                    return 4;
                }
                
                void main() {
                    dict[int, float] d;
                    string x = input();
                    d[5] = f(x, 2, 3.5, {1: 10 as float}) as float;
                    print(d[5] as string);
                }
                """;

        // then
        assertThatNoException().isThrownBy(() -> runSemanticCheck(program));
    }

    @ParameterizedTest
    @CsvSource({
            "int x = 2.0",
            "int x = \"abc\"",
            "int x = \"abc\"",
            "int x = {1: 5.0}",
            "float a = 4",
            "float a = \"4\"",
            "string c = 1",
            "string c = 1.0",
            "float d = f()",
            "string d = f() as int",
    })
    void should_throw_an_error_if_types_in_simple_var_declaration_dont_match(String declaration) {
        // given
        String program = String.format("""
                int f (){
                    return 1;
                }
                
                void main() {
                    %s;
                }
                """, declaration);

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessageStartingWith("Types")
                .withMessageEndingWith("do not match at line 6, column 5");
    }

    @ParameterizedTest
    @CsvSource({
            "int, int, {1: 2.0}",
            "int, int, {\"1\": 5}",
            "int, float, {1: 3}",
            "int, float, {null: 5}",
            "int, float, {5.0: null}",
    })
    void should_throw_an_error_if_types_in_dict_declaration_dont_match(
            String paramType1, String paramType2, String declaration
    ) {
        // given
        String program = String.format("""
                void main() {
                    dict[%s, %s] x = %s;
                }
                """, paramType1, paramType2, declaration);

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessageStartingWith("Types")
                .withMessageEndingWith("do not match at line 2, column 5");
    }

    @Test
    void should_throw_an_error_if_main_function_is_not_defined() {
        // given
        String program = """
                int x = 1;
                int f() {
                    return 1;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Main function is not defined");
    }


    @Test
    void should_throw_an_error_if_main_is_not_void() {
        // given
        String program = """
                int main() {
                    return 0;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Main function should return void");
    }

    @Test
    void should_throw_an_error_if_main_has_any_parameters() {
        // given
        String program = """
                void main(int x) {
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Main function should not have any parameters");
    }

    @Test
    void should_throw_an_error_if_return_statements_is_missing() {
        // given
        String program = """
                int f() {
                }
                
                void main() {
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Missing return statement in function f at line 1, column 1");
    }

    @Test
    void should_throw_an_error_if_return_statement_is_unreachable() {
        // given
        String program = """
                int f() {
                    return 1;
                    return 2;
                }
                
                void main() {
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Unreachable return statement in function f at line 3, column 5");
    }

    @Test
    void should_throw_an_error_when_return_and_function_types_dont_match() {
        // given
        String program = """
                int f(int x) {
                    if (x > 0) {
                        if (x > 10) {
                            return 1.0;
                        }
                
                        return 1;
                    }
            
                    return 1;
                }
                
                void main() {
                int x = f(1);
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Expected function f to return int at line 4, column 13, but received float instead");
    }

    @Test
    void should_throw_an_error_if_variable_is_not_defined_in_var_assignment() {
        // given
        String program = """
                void main() {
                    x = 5;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Variable x is not defined");
    }

    @Test
    void should_throw_an_error_if_variable_is_not_defined_in_dict_assignment() {
        // given
        String program = """
                void main() {
                    y[1] = 5;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Variable y is not defined");
    }

    @Test
    void should_throw_an_error_if_variable_is_not_defined_when_passed_as_function_argument() {
        // given
        String program = """
                void f(int x) {
                }
                
                void main() {
                    f(abc);
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Variable abc is not defined");
    }


    @Test
    void should_throw_an_error_if_condition_in_while_statement_is_invalid() {
        // given
        String program = """
                void main() {
                    while ({1: 2}) {
                    }
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Invalid condition in statement at line 2, column 5");
    }

    @Test
    void should_throw_an_error_if_variable_is_not_defined_when_used_in_operator() {
        // given
        String program = """
                void main() {
                    int a = 1 + b;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Variable b is not defined");
    }

    @Test
    void should_throw_an_error_when_undefined_function_is_called() {
        // given
        String program = """
                void main() {
                    int x = someFun();
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Function someFun is not defined at line 2, column 13");
    }

    @Test
    void should_throw_an_error_when_function_argument_has_invalid_type() {
        // given
        String program = """
                int someFun(int a, float b, string c) {
                    return 1;
                }
                
                void main() {
                    int x = someFun(1, 2.0, 3);
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Types int and string do not match at line 6, column 13");
    }

    @Test
    void should_throw_an_error_when_dict_function_argument_has_invalid_type() {
        // given
        String program = """
                void someFun(dict[string, float] d) {
                }
                
                void main() {
                    dict[string, int] x = {};
                    someFun(x);
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessageStartingWith("Types")
                .withMessageEndingWith("do not match at line 6, column 5");
    }

    @Test
    void should_throw_an_error_when_invalid_number_of_function_arguments_is_provided() {
        // given
        String program = """
                void someFun(int a, int b) {
                }
                
                void main() {
                    someFun(1, 2, 3);
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Function someFun expected 2 arguments, but provided 3 at line 5, column 5");
    }

    @Test
    void should_throw_an_error_when_type_is_not_iterable() {
        // given
        String program = """
                void main() {
                    int x;
                    foreach(int a : x) {
                    }
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Value is not iterable at line 3, column 5");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "int; int; {1: 5, 2: 5.0}",
            "int; float; {1: 5, 2: null, 3: \"x\"}",
    }, delimiter = ';')
    void should_throw_an_error_when_types_in_dict_literal_are_inconsistent(
            String paramType1, String paramType2, String definition
    ) {
        // given
        String program = String.format("""
                void main() {
                    dict[%s, %s] d = %s;
                }
                """, paramType1, paramType2, definition);

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Inconsistent types in dict literal");
    }

    @Test
    void should_throw_an_error_when_cant_apply_unary_minus_operator() {
        // given
        String program = """
                void main() {
                    string x = -"x";
                }
                """;

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Unary minus operator is not applicable to type string");
    }

    @ParameterizedTest
    @CsvSource({
            "int x = {1: 5} as int",
            "float x = {1: 5} as float",
            "string x = {1: 5} as string",
    })
    void should_throw_cast_error_when_casting_a_collection_type(String expression) {
        // given
        String program = String.format("""
                void main() {
                    %s;
                }
                """, expression);

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program))
                .withMessage("Collections can not be casted");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "print()",
            "print(1)",
            "print(\"a\", \"b\")",
            "string x = input(1)",
    }, delimiter = ';')
    void should_throw_an_error_when_builtin_function_has_invalid_arguments(String funCall) {
        // given
        String program = String.format("""
                void main() {
                    %s;
                }
                """, funCall);

        // then
        assertThatExceptionOfType(DefaultSemanticChecker.SemanticException.class)
                .isThrownBy(() -> runSemanticCheck(program));
    }

    private void runSemanticCheck(String input) {
        Parser parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));
        Program program = parser.parseProgram();
        semanticChecker.verify(program);
    }
}