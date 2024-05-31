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
                    print(x);
                    print(y as string);
                    print(z as string);
                }
                """;

        // when
        String capturedOutput = executeProgramAndCaptureOutput(program);

        // then
        assertThat(capturedOutput).isEqualTo("1\n2\n3.0");
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

    private String executeProgramAndCaptureOutput(String input) {
        Parser parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));
        Program program = parser.parseProgram();
        programExecutor.execute(program);

        return outContent.toString().trim();
    }
}
