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

import static org.assertj.core.api.Assertions.assertThat;

public class ProgramExecutorTest {
    private final ProgramExecutor programExecutor = new DefaultProgramExecutor();
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
                    int a = 10;
                    float b = 1.0;
                    string c;
                    int d = null;

                    a = 5;
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
        assertThat(capturedOutput).isEqualTo("5\nnull\nxyz\n1");
    }

    private String executeProgramAndCaptureOutput(String input) {
        Parser parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));
        Program program = parser.parseProgram();
        programExecutor.execute(program);

        return outContent.toString().trim();
    }
}
