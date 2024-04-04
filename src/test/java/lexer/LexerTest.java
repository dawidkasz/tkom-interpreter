package lexer;

import static lexer.Lexer.LexerException;

import lexer.characterprovider.StringCharacterProvider;
import org.assertj.core.util.FloatComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static lexer.TokenTypeMapper.DOUBLE_SIGN_OPERATORS;
import static lexer.TokenTypeMapper.KEYWORDS;
import static lexer.TokenTypeMapper.SINGLE_SIGN_OPERATORS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LexerTest {
    @ParameterizedTest
    @CsvSource({"x", "xyz", "_xyz", "_", "_x_y_z", "x33", "_1", "intx", "xint", "if_"})
    void should_match_identifier_when_initializing_a_variable(String variableName) {
        // given
        String text = String.format("int %s =  123 ;", variableName);

        // when
        List<Token> tokens = tokenize(text);

        // then
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

    @Test
    void should_not_allow_identifiers_longer_than_128_characters() {
        // given
        String tooLongIdentifier = "x".repeat(129);
        String text = String.format("int %s;", tooLongIdentifier);

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(text))
                .withMessage("Identifier length exceeded the limit of 128 characters (line=1, column=5)");
    }

    @ParameterizedTest
    @MethodSource("provideKeywordsForKeywordMatchingTest")
    void should_match_keywords(String keywordName) {
        // given
        String text = String.format("    \n\n  %s \n", keywordName);

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(KEYWORDS).hasSize(12);

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(KEYWORDS.get(keywordName)))
                .matches(t -> t.value().equals(keywordName))
                .extracting(Token::position)
                .matches(p -> p.lineNumber() == 3 && p.columnNumber() == 3);
    }

    private static Stream<Arguments> provideKeywordsForKeywordMatchingTest() {
        return KEYWORDS.keySet().stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForStringLiteralMatchingTest")
    void should_match_string_literals(String escapedString, String expectedValue) {
        // when
        List<Token> tokens = tokenize(escapedString);
        System.out.println(escapedString);
        System.out.println(tokens);
        // then
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
                Arguments.of("\"\\\"  \\\"\\n \\\\ x \"", "\"  \"\n \\ x "),
                Arguments.of("\"x \n y\"", "x \n y")
        );
    }

    @Test
    void should_throw_an_error_when_escaping_an_illegal_character() {
        // given
        String text = "\n\nint x = \"\\x\"";
        String expectedMessage = "Illegal escape character '\\x' (line=3, column=11)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(text))
                .withMessage(expectedMessage);
    }

    @Test
    void should_throw_an_error_when_string_literal_length_is_exceeded() {
        // given
        String text = "\"" + "x".repeat(1025) + "\"";
        String expectedMessage = "String literal length exceeded the limit of 1024 characters (line=1, column=1)";


        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(text))
                .withMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({"0", "4", "2500", "2147483647"})
    void should_match_int_literals(String intLiteral) {
        // when
        List<Token> tokens = tokenize(intLiteral);

        // then
        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(Integer.valueOf(intLiteral)));
    }

    @ParameterizedTest
    @CsvSource({"2147483648"})
    void should_detect_overflow_in_number_literals(String numberLiteral) {
        // given
        String expectedMessage = "Overflow: number literal exceeded its maximum value (line=1, column=1)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(numberLiteral))
                .withMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({"00", "000", "03", "009", "00.0", "000.992", "003.5"})
    void should_throw_an_error_if_there_are_leading_zeros_in_a_number_literal(String numberLiteral) {
        // given
        String expectedMessage = "Leading zeros in number literal (line=1, column=1)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(numberLiteral))
                .withMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({"0.0", "0.1", "0.0008", "1.0", "0.42013", "5002710.58769", "1.000"})
    void should_match_float_literals(String floatLiteral) {
        // when
        List<Token> tokens = tokenize(floatLiteral);

        // then
        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(TokenType.FLOAT_LITERAL))
                .extracting(Token::value)
                .matches(v -> new FloatComparator(1e-6f).compare((Float) v, Float.parseFloat(floatLiteral)) == 0);
    }

    @Test
    void should_throw_an_error_when_float_literal_doesnt_have_a_fractional_part_after_dot() {
        // when
        String text = "float x = 123.;";
        String expectedMessage = "Missing fractional part in a float literal (line=1, column=11)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(text))
                .withMessage(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("provideOperatorsForOperatorsMatchingTest")
    void should_match_operators(String operator) {
        // given
        TokenType expectedTokenType = Optional.ofNullable(SINGLE_SIGN_OPERATORS.get(operator)).orElse(DOUBLE_SIGN_OPERATORS.get(operator));

        // when
        List<Token> tokens = tokenize(operator);

        // then
        assertThat(SINGLE_SIGN_OPERATORS).hasSize(19);
        assertThat(DOUBLE_SIGN_OPERATORS).hasSize(6);

        assertThat(tokens)
                .first()
                .matches(t -> t.tokenType().equals(expectedTokenType))
                .matches(t -> t.value().equals(operator));
    }

    private static Stream<Arguments> provideOperatorsForOperatorsMatchingTest() {
        Stream<String> singleOperatorStream = SINGLE_SIGN_OPERATORS.keySet().stream();
        Stream<String> doubleOperatorStream = DOUBLE_SIGN_OPERATORS.keySet().stream();

        return Stream.concat(singleOperatorStream, doubleOperatorStream).map(Arguments::of);
    }

    @Test
    void should_process_ambiguous_operators() {
        // given
        String text = "<<====>>===";

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.LESS_THAN_OPERATOR,
                TokenType.LESS_THAN_OR_EQUAL_OPERATOR,
                TokenType.EQUAL_OPERATOR,
                TokenType.ASSIGNMENT,
                TokenType.GREATER_THAN_OPERATOR,
                TokenType.GREATER_THAN_OR_EQUAL_OPERATOR,
                TokenType.EQUAL_OPERATOR
        );
    }

    @ParameterizedTest
    @CsvSource({"|", "&"})
    void should_throw_an_error_if_operator_is_invalid(String invalidOperator) {
        // given
        String expectedMessage = "Invalid operator (line=1, column=1)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(invalidOperator))
                .withMessage(expectedMessage);
    }

    @Test
    void should_throw_an_error_when_token_cant_be_processed() {
        // given
        String text = "int x = ';";
        String expectedMessage = "Unrecognized token (line=1, column=9)";

        // then
        assertThatExceptionOfType(LexerException.class)
                .isThrownBy(() -> tokenize(text))
                .withMessage(expectedMessage);
    }

    @Test
    void should_skip_white_characters_and_track_position() {
        //given
        String text = """
                
        int          x
        
        
          =
          
             21
        \t;
        
        """;

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens)
                .extracting(Token::tokenType)
                .containsExactly(TokenType.INT_KEYWORD, TokenType.IDENTIFIER, TokenType.ASSIGNMENT, TokenType.INT_LITERAL, TokenType.SEMICOLON);

        assertThat(tokens)
                .extracting(Token::value)
                .containsExactly("int", "x", "=", 21, ";");

        assertThat(tokens)
                .extracting(Token::position)
                .containsExactly(
                        new Position(2, 1),
                        new Position(2, 14),
                        new Position(5, 3),
                        new Position(7, 6),
                        new Position(8,2)
                );
    }

    @Test
    void should_process_expression_assignment() {
        // given
        String text = "int x = 1 * (2 + 2) / 3 - 24 % 2.0 as int;";

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.INT_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.ASSIGNMENT,
                TokenType.INT_LITERAL,
                TokenType.MULTIPLICATION_OPERATOR,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.INT_LITERAL,
                TokenType.PLUS_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.DIVISION_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.MINUS_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.MODULO_OPERATOR,
                TokenType.FLOAT_LITERAL,
                TokenType.AS_KEYWORD,
                TokenType.INT_KEYWORD,
                TokenType.SEMICOLON
        );

        assertThat(tokens.get(13))
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(24))
                .matches(t -> t.position().equals(new Position(1, 27)));
    }

    @Test
    void should_process_dict_assignment() {
        // given
        String text = "dict[int, string] map = {1: \"a\\\"b\"};";

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.DICT_KEYWORD,
                TokenType.LEFT_SQUARE_BRACKET,
                TokenType.INT_KEYWORD,
                TokenType.COMMA,
                TokenType.STRING_KEYWORD,
                TokenType.RIGHT_SQUARE_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.ASSIGNMENT,
                TokenType.LEFT_CURLY_BRACKET,
                TokenType.INT_LITERAL,
                TokenType.COLON,
                TokenType.STRING_LITERAL,
                TokenType.RIGHT_CURLY_BRACKET,
                TokenType.SEMICOLON
        );

        assertThat(tokens.get(11))
                .matches(t -> t.tokenType().equals(TokenType.STRING_LITERAL))
                .matches(t -> t.value().equals("a\"b"))
                .matches(t -> t.position().equals(new Position(1, 29)));
    }

    @Test
    void should_process_if_statement() {
        // given
        String text = """
            if (x <= 6 || 2 == 3 && 2 != abc()) {
                print(1 as string);
            }
            """;

        // when
        List<Token> tokens = tokenize(text );

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.IF_KEYWORD,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LESS_THAN_OR_EQUAL_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.OR_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.EQUAL_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.AND_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.NOT_EQUAL_OPERATOR,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.LEFT_CURLY_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.INT_LITERAL,
                TokenType.AS_KEYWORD,
                TokenType.STRING_KEYWORD,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.SEMICOLON,
                TokenType.RIGHT_CURLY_BRACKET
        );

        assertThat(tokens.get(17))
                .matches(t -> t.tokenType().equals(TokenType.IDENTIFIER))
                .matches(t -> t.value().equals("print"))
                .matches(t -> t.position().equals(new Position(2, 5)));

    }

    @Test
    void should_process_while_statement() {
        // given
        String text = """
            while(x<5) {
                process(some_map[id(x)]?);
                x = x + 1;
            }
            """;

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.WHILE_KEYWORD,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LESS_THAN_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.LEFT_CURLY_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_SQUARE_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.RIGHT_SQUARE_BRACKET,
                TokenType.NULLABLE_OPERATOR,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.SEMICOLON,
                TokenType.IDENTIFIER,
                TokenType.ASSIGNMENT,
                TokenType.IDENTIFIER,
                TokenType.PLUS_OPERATOR,
                TokenType.INT_LITERAL,
                TokenType.SEMICOLON,
                TokenType.RIGHT_CURLY_BRACKET
        );
    }

    @Test
    void should_process_foreach_statement() {
        // given
        String text = "foreach (int key: mp) { print(mp[key]); }";

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.FOREACH_KEYWORD,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.INT_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.COLON,
                TokenType.IDENTIFIER,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.LEFT_CURLY_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.LEFT_SQUARE_BRACKET,
                TokenType.IDENTIFIER,
                TokenType.RIGHT_SQUARE_BRACKET,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.SEMICOLON,
                TokenType.RIGHT_CURLY_BRACKET
        );
    }

    @Test
    void should_process_function_definition() {
        // given
        String text = """
            void add(int a, int b)
            {
                return a + b;
            }
            """;

        // when
        List<Token> tokens = tokenize(text);

        // then
        assertThat(tokens).extracting(Token::tokenType).containsExactly(
                TokenType.VOID_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.LEFT_ROUND_BRACKET,
                TokenType.INT_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.COMMA,
                TokenType.INT_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.RIGHT_ROUND_BRACKET,
                TokenType.LEFT_CURLY_BRACKET,
                TokenType.RETURN_KEYWORD,
                TokenType.IDENTIFIER,
                TokenType.PLUS_OPERATOR,
                TokenType.IDENTIFIER,
                TokenType.SEMICOLON,
                TokenType.RIGHT_CURLY_BRACKET
        );
    }

    @Test
    void should_return_eof_as_last_token() {
        // given
        Lexer lexer = new Lexer(new StringCharacterProvider("\n x \n 123  \n "));

        // when
        List<Token> tokens = List.of(lexer.nextToken(), lexer.nextToken(), lexer.nextToken(), lexer.nextToken());

        // then
        assertThat(tokens)
                .extracting(Token::tokenType)
                .containsExactly(TokenType.IDENTIFIER, TokenType.INT_LITERAL, TokenType.EOF, TokenType.EOF);
    }

    private List<Token> tokenize(String input) {
        Lexer lexer = new Lexer(new StringCharacterProvider(input));

        return Stream.generate(lexer::nextToken)
                .takeWhile(t -> !t.tokenType().equals(TokenType.EOF))
                .collect(Collectors.toList());
    }
}
