package lexer;

import lexer.characterprovider.FileCharacterProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


public class LexerTest {
    @TempDir
    File baseDir;

    @Test
    void should_tokenize_input_from_a_file() {
        // given
        String filePath = prepareInputFile("int x = null;");

        // when
        List<Token> tokens = tokenize(filePath);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.INT_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.ASSIGNMENT,
                TokenType.NULL_KEYWORD,
                TokenType.SEMICOLON
        );

        assertThat(tokens).extracting(Token::position).containsExactly(
                new Position(1, 1),
                new Position(1, 5),
                new Position(1, 7),
                new Position(1, 9),
                new Position(1, 13)
        );
    }

    @Test
    void should_track_position_in_a_file() {
        // given
        String filePath = prepareInputFile(
                """
                
                x       \ty
                
                
                
                   z
                """
        );

        // when
        List<Token> tokens = tokenize(filePath);

        // then
        assertThat(tokens).extracting(Token::position).containsExactly(
                new Position(2, 1),
                new Position(2, 10),
                new Position(6, 4)
        );
    }

    private String prepareInputFile(String content) {
        File inputFile = new File(baseDir, "in.txt");

        try (PrintWriter printWriter = new PrintWriter(inputFile)) {
            printWriter.write(content);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return inputFile.getAbsolutePath();
    }

    private List<Token> tokenize(String filePath) {
        Lexer lexer = new DefaultLexer(new FileCharacterProvider(filePath));

        return Stream.generate(lexer::nextToken)
                .takeWhile(t -> !t.tokenType().equals(TokenType.EOF))
                .collect(Collectors.toList());
    }
}
