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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProgramExecutorTest {
    private final ProgramExecutor programExecutor = new DefaultProgramExecutor(new DefaultSemanticChecker());
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
    void should_execute_builtin_print() {
        // given
        String program = """
                void main() {
                    print("xyz");
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("xyz");
    }

    @Test
    void should_execute_builtin_input() {
        // given
        String program = """
                void main() {
                    string val = input();
                    print("Read value: " + val);
                }
                """;
        String input = "hello world";
        String capturedOutput;

        // when
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            capturedOutput = executeProgramAndCaptureOutput(program);
        } finally {
            System.setIn(System.in);
        }

        // then
        assertThat(capturedOutput).isEqualTo("Read value: hello world");
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "1.0, 1.0",
            "\"x\", x",
            "(\"1\" as int + 1.0 as int), 2",
            "(1 as float + \"2.0\" as float), 3.0",
    })
    void should_cast_simple_types(String input, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print(%s as string);
                }
                """, input);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @Test
    void should_declare_local_variables() {
        // given
        String program = """
                void main() {
                    string x = "1";
                    int y = 2;
                    float z = 3.0;
                    int t = null;
                    print(x);
                    print(y as string);
                    print(z as string);
                    print(t as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1\n2\n3.0\nnull");
    }

    @ParameterizedTest
    @CsvSource({
            "int, 1, 1, 2",
            "float, 1.0, 2.5, 3.5",
            "string, \"a\", \"b\", ab",
    })
    void should_execute_plus_expression(String type, String a, String b, String expected) {
        // given
        String program = String.format("""
                void main() {
                    %s x = %s + %s;
                    print(x as string);
                }
                """, type, a, b);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "int, 5, 1, 4",
            "float, 3.0, 0.5, 2.5",
    })
    void should_execute_minus_expression(String type, String a, String b, String expected) {
        // given
        String program = String.format("""
                void main() {
                    %s x = %s - %s;
                    print(x as string);
                }
                """, type, a, b);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1 + null",
            "null - null",
            "null + null",
            "null",
    })
    void should_nullify_whole_expression_when_nullable_operator_is_used(String expression) {
        // given
        String program = String.format("""
                void main() {
                    print(((%s)?) as string);
                }
                """, expression);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("null");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 2, 5",
            "18.0, 4.5, 4.0",
    })
    void should_execute_divide_expression(String a, String b, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print((%s / %s) as string);
                }
                """, a, b);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1 < 2, 1",
            "2.0 < 1.0, 0",
            "5.0 > 4.0, 1",
            "2 > 3, 0",
            "3 <= 3, 1",
            "3.0 <= 2.9, 0",
            "5.0 >= 5.0, 1",
            "4 >= 5, 0",
            "1 && 2.0, 1",
            "\"x\" && 5, 1",
            "0 && \"\", 0",
            "2 && \"\", 0",
            "0.0 && 0.0, 0",
            "2 && null, 0",
            "1 || 0, 1",
            "0.0 || 1, 1",
            "null || 1, 1",
            "0 || \"\", 0",
            "null || null, 0",
            "1 == 1, 1",
            "5.0 == 5.0, 1",
            "\"abc\" == \"abc\", 1",
            "1 == 2, 0",
            "5.0 == 6.0, 0",
            "\"abc\" == \"def\", 0",
            "1 != 1, 0",
            "5.0 != 5.0, 0",
            "\"abc\" != \"abc\", 0",
            "1 != 2, 1",
            "5.0 != 6.0, 1",
            "\"abc\" != \"def\", 1"
    })
    void should_execute_relational_expressions(String expression, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print((%s) as string);
                }
                """, expression);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 0",
            "0, 1",
            "\"\", 1",
            "\"x\", 0",
            "1 < 2, 0",
            "2.0 <= 1.0, 1",
            "null, 1",
            "!null, 0",
    })
    void should_execute_negation_expression(String expression, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print((!(%s)) as string);
                }
                """, expression);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "8, 3, 2",
            "8.0, 3.0, 2.0",
    })
    void should_execute_modulo_expression(String a, String b, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print((%s %% %s) as string);
                }
                """, a, b);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "7, -7",
            "8.25, -8.25",
    })
    void should_execute_unary_minus_expression(String expression, String expected) {
        // given
        String program = String.format("""
                void main() {
                    print((-(%s)) as string);
                }
                """, expression);

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expected);
    }

    @Test
    void should_assign_variables() {
        // given
        String program = """
                void main() {
                    int tmp = 1;

                    int a = 10;
                    float b = 1.0;
                    string c;
                    int d = null;

                    a = a + 5 + tmp;
                    b = null;
                    c = "xyz";
                    d = 1;

                    print(a as string);
                    print(b as string);
                    print(c as string);
                    print(d as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("16\nnull\nxyz\n1");
    }

    @Test
    void should_execute_if_statement() {
        // given
        String program = """
                void main() {
                    if (1 == 1) {
                        print("a");
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("a");
    }

    @Test
    void should_not_execute_if_there_is_no_else_statement() {
        // given
        String program = """
                void main() {
                    if (1 == 2) {
                        print("a");
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("");
    }

    @Test
    void should_execute_else_statement() {
        // given
        String program = """
                void main() {
                    if (1 == 2) {
                        print("a");
                    } else {
                        print("b");
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("b");
    }

    @Test
    void should_execute_while_statement() {
        // given
        String program = """
                void main() {
                    int x = 1;
                    while (x <= 5) {
                        print(x as string);
                        x = x + 1;
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1\n2\n3\n4\n5");
    }

    @Test
    void should_execute_function_call() {
        // given
        String program = """
                int addOrZero(int a, int b) {
                    if (a == 0 || b == 0) {
                        return 0;
                    }

                    return a + b;
                }
                
                void f2(){
                    print("b");
                }

                void main() {
                    int a = addOrZero(1, 0);
                    print(a as string);
                    f2();
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("0\nb");
    }

    @Test
    void should_support_recursion() {
        // given
        String program = """
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
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1\n55");
    }

    @Test
    void should_not_execute_any_statements_after_return() {
        // given
        String program = """
                void f() {
                    if (1) {
                        print("a");
                        return null;
                    }
                
                    print("b");
                    return null;
                }
                
                void main() {
                    f();
                    if (1) {
                        print("c");
                        return null;
                    }
                    print("d");
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("a\nc");
    }

    @Test
    void should_correctly_deduce_variable_scope() {
        // given
        String program = """
                string x = "a";
                
                void f() {
                    string x = "b";
                    print(x);
                
                    if (1) {
                        string x = "c";
                        print(x);
                
                        if (1) {
                            x = "c2";
                        }
                
                        print(x);
                    }
                
                    x = "b2";
                    print(x);
                }
                
                void main() {
                    print(x);
                    f();
                    print(x);
                    x = "a2";
                    print(x);
                }
                """;

        String expectedOutput = "a\nb\nc\nc2\nb2\na\na2";

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo(expectedOutput);
    }

    @Test
    void should_declare_dict_variables() {
        // given
        String program = """
                dict[string, float] a = {"x": 1.0};
                
                void main() {
                    dict[int, string] b = {1: "a", 2: "b"};
                    dict[int, int] c;
                
                    print(a["x"] as string);
                    print(b[1]);
                    print({2: 10}[2] as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1.0\na\n10");
    }

    @Test
    void should_handle_dict_assignments() {
        // given
        String program = """
                dict[string, float] a;
                
                void main() {
                    dict[int, int] b = {1: 10, 2: 20};
                
                    print(b[1] as string);
                
                    a["123"] = 3.0;
                    b[1] = 11;
                
                    print(a["123"] as string);
                    print(b[1] as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("10\n3.0\n11");
    }

    @Test
    void should_throw_null_pointer_exception_when_dict_key_is_not_present() {
        // given
        String program = """                
                void main() {
                    dict[int, int] x = {1: 2};
                    int a = x[1];
                    int b = x[234];
                }
                """;

        // then
        assertThatExceptionOfType(DefaultProgramExecutor.AppNullPointerException.class)
                .isThrownBy(() -> executeProgramAndCaptureOutput(program))
                .withMessageStartingWith("Key '234' doesn't exist");
    }

    @Test
    void should_not_throw_null_pointer_exception_when_dict_key_is_not_present_but_exception_is_handled() {
        // given
        String program = """                
                void main() {
                    dict[int, int] x = {1: 2};
                    int a = x[1];
                    int b = x[234]?;
                
                    print(b as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("null");
    }

    @Test
    void should_pass_dict_arguments_by_reference() {
        // given
        String program = """
                void changeDict1(dict[int, string] d) {
                    d[1] = "b";
                }
                
                void changeDict2(dict[int, string] x) {
                    x[1] = "c";
                }
                
                void main() {
                    dict[int, string] d = {1: "a"};
                    print(d[1]);
                
                    changeDict1(d);
                    print(d[1]);
                
                    changeDict2(d);
                    print(d[1]);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("a\nb\nc");
    }

    @Test
    void should_pass_simple_arguments_by_value() {
        // given
        String program = """
                void changeInt1(int a) {
                    a = 2;
                }
                
                void changeInt2(int x) {
                    x = 3;
                }
                
                void main() {
                    int a = 1;
                    print(a as string);
                
                    changeInt1(a);
                    print(a as string);
                
                    changeInt2(a);
                    print(a as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1\n1\n1");
    }

    @Test
    void should_execute_foreach_statement() {
        // given
        String program = """
                void main() {
                    dict[int, string] d = {5: "a"};
                    d[1] = "d";
                    d[3] = "b";
                    d[2] = "e";
                
                    foreach(int i : d) {
                        print(d[i]);
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("a\nd\nb\ne");
    }

    @ParameterizedTest
    @CsvSource({
            "null + null",
            "2 + null",
            "null + 2.0",
            "\"x\" + null",
            "-null",
            "null * 6",
            "null * 6.0",
            "null / 5.5",
            "null / 4",
            "4 % null",
            "null * null",
            "null % 3.5",
            "1 + f()",
            "null < 5",
            "8 <= null",
            "f() > 2",
            "null >= 3.3",
    })
    void should_throw_null_pointer_exception_when_null_is_in_binary_operation(String operation) {
        // given
        String program = String.format("""
                int f() {
                    return null;
                }
                
                void main() {
                    string result = (%s) as string;
                }
                """, operation);

        // then
        assertThatExceptionOfType(DefaultProgramExecutor.AppNullPointerException.class)
                .isThrownBy(() -> executeProgramAndCaptureOutput(program));
    }

    @Test
    void should_throw_an_error_when_dividing_by_zero() {
        // given
        String program = """
                void main() {
                    int x = 1 / 0;
                }
                """;

        // then
        assertThatExceptionOfType(DefaultProgramExecutor.AppZeroDivisionError.class)
                .isThrownBy(() -> executeProgramAndCaptureOutput(program));
    }

    @Test
    void should_only_execute_necessary_expressions_in_or_statement() {
        // given
        String program = """
                int f(int x) {
                    print(x as string);
                    return x;
                }
                
                void main() {
                    if(f(0) || f(0) || f(0) || f(1) || f(1) || f(1)) {
                        print("x");
                    }
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("0\n0\n0\n1\nx");
    }

    private String executeProgramAndCaptureOutput(String input) {
        Parser parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));
        Program program = parser.parseProgram();
        programExecutor.execute(program);

        return outContent.toString().trim();
    }
}
