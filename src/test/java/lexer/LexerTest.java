package lexer;

import org.assertj.core.util.FloatComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static lexer.TokenTypeMapper.KEYWORDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

public class LexerTest {
    @ParameterizedTest
    @CsvSource({"x", "xyz", "_xyz", "_", "_x_y_z", "x33", "_1", "intx", "xint"})
    void should_match_identifier_when_initializing_a_variable(String variableName) {
        List<Token> tokens = tokenize(String.format("int %s =  123 ;", variableName));

        assertThat(tokens).hasSize(5);

        assertThat(tokens.get(0)).extracting(Token::tokenType).isEqualTo(TokenType.INT_KEYWORD);

        assertThat(tokens.get(1))
                .matches(t -> t.tokenType().equals(TokenType.IDENTIFIER))
                .matches(t -> t.value().equals(variableName));

        assertThat(tokens.get(2)).extracting(Token::tokenType).isEqualTo(TokenType.ASSIGNMENT);

        assertThat(tokens.get(3))
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(123));

        assertThat(tokens.get(4)).extracting(Token::tokenType).isEqualTo(TokenType.SEMICOLON);
    }

    @ParameterizedTest
    @CsvSource({"if", "else", "while", "foreach", "return", "as", "void", "int", "float", "string", "dict", "null"})
    void should_match_keywords(String keywordName) {
        List<Token> tokens = tokenize(String.format("    \n\n  %s \n", keywordName));

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(KEYWORDS.get(keywordName)))
                .matches(t -> t.value().equals(keywordName))
                .extracting(Token::position)
                .matches(p -> p.lineNumber() == 3 && p.columnNumber() == 3);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForStringLiteralMatchingTest")
    void should_match_string_literals(String escapedString, String expectedValue) {
        System.out.println(escapedString);
        System.out.println(expectedValue);
        List<Token> tokens = tokenize(escapedString);

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(TokenType.STRING_LITERAL))
                .matches(t -> t.value().equals(expectedValue));
    }

    private static Stream<Arguments> provideStringsForStringLiteralMatchingTest() {
        return Stream.of(
                Arguments.of("\"x y z\"", "x y z"),
                Arguments.of("\"aa\\n x \\t\\nbb\"", "aa\n x \t\nbb"),
                Arguments.of("\"\\\\\"", "\\"),
                Arguments.of("\"\\\"  \\\"\\n \\\\ x \"", "\"  \"\n \\ x ")
        );
    }


    @ParameterizedTest
    @CsvSource({"0", "4", "2500", "1234567890"})
    void should_match_int_literals(String intLiteral) {
        List<Token> tokens = tokenize(intLiteral);

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(Integer.valueOf(intLiteral)));
    }

    @ParameterizedTest
    @CsvSource({"0.0", "0.1", "0.0008", "1.0", "0.42013", "5002710.58769"})
    void should_match_float_literals(String floatLiteral) {
        List<Token> tokens = tokenize(floatLiteral);

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(TokenType.FLOAT_LITERAL))
                .extracting(Token::value)
                .matches(v -> new FloatComparator(1e-6f).compare((Float) v, Float.parseFloat(floatLiteral)) == 0);
    }

    @Test
    void should_tokenize_identifier_when_overlaps_with_keyword() {
        List<Token> tokens = tokenize("int intxxx;");

        assertThat(tokens).hasSize(3);

        assertThat(tokens.get(0)).matches(t -> t.tokenType().equals(TokenType.INT_KEYWORD));
        assertThat(tokens.get(1))
                .matches(t -> t.tokenType().equals(TokenType.IDENTIFIER))
                .matches(t -> t.value().equals("intxxx"));
        assertThat(tokens.get(2)).matches(t -> t.tokenType().equals(TokenType.SEMICOLON));
    }



    @Test
    void test_expression_assignment() {
        List<Token> tokens = tokenize("int x = 1 * (2 + 2) / 3 - 4 % 2;");

        assertThat(tokens).hasSize(17);

        assertThat(tokens.get(3))
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(1));

        assertThat(tokens.get(4)).matches(t -> t.tokenType().equals(TokenType.MULTIPLICATION_OPERATOR));

        assertThat(tokens.get(5)).matches(t -> t.tokenType().equals(TokenType.LEFT_ROUND_BRACKET));

        assertThat(tokens.get(6)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));

        assertThat(tokens.get(7)).matches(t -> t.tokenType().equals(TokenType.PLUS_OPERATOR));

        assertThat(tokens.get(8)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));

        assertThat(tokens.get(9)).matches(t -> t.tokenType().equals(TokenType.RIGHT_ROUND_BRACKET));

        assertThat(tokens.get(10)).matches(t -> t.tokenType().equals(TokenType.DIVISION_OPERATOR));

        assertThat(tokens.get(11)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));

        assertThat(tokens.get(12)).matches(t -> t.tokenType().equals(TokenType.MINUS_OPERATOR));

        assertThat(tokens.get(13)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));

        assertThat(tokens.get(14)).matches(t -> t.tokenType().equals(TokenType.MODULO_OPERATOR));

        assertThat(tokens.get(15)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));
    }

    @Test
    void test_if_statement() {
        List<Token> tokens = tokenize("""
            int x = 5;
            if (x <= 6 || 2 == 3 && 2 != 3) {
                print(1 as string);
            }
        """);

        assertThat(tokens).hasSize(28);

        assertThat(tokens.get(5)).matches(t -> t.tokenType().equals(TokenType.IF_KEYWORD));
        assertThat(tokens.get(8)).matches(t -> t.tokenType().equals(TokenType.LESS_THAN_OR_EQUAL_OPERATOR));
        assertThat(tokens.get(10)).matches(t -> t.tokenType().equals(TokenType.OR_OPERATOR));
        assertThat(tokens.get(12)).matches(t -> t.tokenType().equals(TokenType.EQUAL_OPERATOR));
        assertThat(tokens.get(14)).matches(t -> t.tokenType().equals(TokenType.AND_OPERATOR));
        assertThat(tokens.get(16)).matches(t -> t.tokenType().equals(TokenType.NOT_EQUAL_OPERATOR));
        assertThat(tokens.get(19)).matches(t -> t.tokenType().equals(TokenType.LEFT_CURLY_BRACKET));

        assertThat(tokens.get(20))
                .matches(t -> t.tokenType().equals(TokenType.IDENTIFIER))
                .matches(t -> t.value().equals("print"));

        assertThat(tokens.get(23)).matches(t -> t.tokenType().equals(TokenType.AS_KEYWORD));

        assertThat(tokens.get(24)).matches(t -> t.tokenType().equals(TokenType.STRING_KEYWORD));

        assertThat(tokens.get(27)).matches(t -> t.tokenType().equals(TokenType.RIGHT_CURLY_BRACKET));

    }

    @Test
    void test_dict_assignment() {
        List<Token> tokens = tokenize("dict[int, int] map = {1: 2};");

        assertThat(tokens).hasSize(14);

        assertThat(tokens.get(0)).matches(t -> t.tokenType().equals(TokenType.DICT_KEYWORD));
        assertThat(tokens.get(1)).matches(t -> t.tokenType().equals(TokenType.LEFT_SQUARE_BRACKET));
        assertThat(tokens.get(2)).matches(t -> t.tokenType().equals(TokenType.INT_KEYWORD));
        assertThat(tokens.get(3)).matches(t -> t.tokenType().equals(TokenType.COMMA));
        assertThat(tokens.get(4)).matches(t -> t.tokenType().equals(TokenType.INT_KEYWORD));
        assertThat(tokens.get(5)).matches(t -> t.tokenType().equals(TokenType.RIGHT_SQUARE_BRACKET));
        assertThat(tokens.get(6)).matches(t -> t.tokenType().equals(TokenType.IDENTIFIER));
        assertThat(tokens.get(7)).matches(t -> t.tokenType().equals(TokenType.ASSIGNMENT));
        assertThat(tokens.get(8)).matches(t -> t.tokenType().equals(TokenType.LEFT_CURLY_BRACKET));
        assertThat(tokens.get(9)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));
        assertThat(tokens.get(10)).matches(t -> t.tokenType().equals(TokenType.COLON));
        assertThat(tokens.get(11)).matches(t -> t.tokenType().equals(TokenType.INT_LITERAL));
        assertThat(tokens.get(12)).matches(t -> t.tokenType().equals(TokenType.RIGHT_CURLY_BRACKET));
        assertThat(tokens.get(13)).matches(t -> t.tokenType().equals(TokenType.SEMICOLON));
    }

    private List<Token> tokenize(String input) {
        var lexer = new Lexer(new StringCharacterProvider(input));

        List<Token> tokens = new ArrayList<>();

        Token token;
        while ((token = lexer.nextToken()).tokenType() != TokenType.EOF) {
            System.out.println(token);
            tokens.add(token);
        }

        return tokens;
    }
}
