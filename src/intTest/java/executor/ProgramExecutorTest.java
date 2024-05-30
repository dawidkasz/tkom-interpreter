package executor;

import ast.Program;
import lexer.DefaultLexer;
import lexer.characterprovider.StringCharacterProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private String executeProgramAndCaptureOutput(String input) {
        Parser parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));
        Program program = parser.parseProgram();
        programExecutor.execute(program);

        return outContent.toString().trim();
    }
}
