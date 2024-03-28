package lexer;

import org.junit.jupiter.api.Test;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LexerTest {
    @Test
    void test_simple_variable_declaration() {
        List<Token> tokens = tokenize("int _x_y1z =  4321 ;");

        assertThat(tokens).hasSize(5);

        assertThat(tokens.get(0)).matches(t -> t.tokenType().equals(TokenType.INT_KEYWORD));

        assertThat(tokens.get(1))
                .matches(t -> t.tokenType().equals(TokenType.IDENTIFIER))
                .matches(t -> t.value().equals("_x_y1z"));

        assertThat(tokens.get(2)).matches(t -> t.tokenType().equals(TokenType.ASSIGNMENT));

        assertThat(tokens.get(3))
                .matches(t -> t.tokenType().equals(TokenType.INT_LITERAL))
                .matches(t -> t.value().equals(4321));

        assertThat(tokens.get(4)).matches(t -> t.tokenType().equals(TokenType.SEMICOLON));
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
