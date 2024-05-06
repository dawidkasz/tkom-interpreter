package lexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static lexer.TokenType.COMMA;
import static lexer.TokenType.IDENTIFIER;
import static lexer.TokenType.LEFT_CURLY_BRACKET;
import static lexer.TokenType.LEFT_ROUND_BRACKET;
import static lexer.TokenType.LEFT_SQUARE_BRACKET;
import static lexer.TokenType.RIGHT_CURLY_BRACKET;
import static lexer.TokenType.RIGHT_ROUND_BRACKET;
import static lexer.TokenType.RIGHT_SQUARE_BRACKET;

public class TokenFactory {
    private TokenFactory() {
    }

    public static List<Token> program(List<List<Token>> tokenLists) {
        var tokens = flatten(tokenLists);
        tokens.add(Token.eof());

        return tokens;
    }

    public static List<Token> function(TokenType returnType, String name, List<Token> params, List<Token> content) {
        var tokens = new ArrayList<>(List.of(getToken(returnType), getToken(IDENTIFIER, name), getToken(LEFT_ROUND_BRACKET)));
        tokens.addAll(params);
        tokens.add(getToken(RIGHT_ROUND_BRACKET));
        tokens.add(getToken(LEFT_CURLY_BRACKET));
        tokens.addAll(content);
        tokens.add(getToken(RIGHT_CURLY_BRACKET));
        return tokens;
    }

    public static List<Token> parameter(TokenType type, String name) {
        return List.of(getToken(type), getToken(IDENTIFIER, name));
    }

    public static List<Token> parameter(TokenType type, TokenType t1, TokenType t2, String name) {
        return List.of(getToken(type), getToken(LEFT_SQUARE_BRACKET), getToken(t1), getToken(COMMA),
                getToken(t2), getToken(RIGHT_SQUARE_BRACKET), getToken(IDENTIFIER, name));
    }

    public static List<Token> parameters(List<List<Token>> params) {
        List<Token> tokens = new ArrayList<>();
        for(int i = 0; i < params.size(); ++i) {
            tokens.addAll(params.get(i));
            if (i < params.size() - 1) {
                tokens.add(getToken(COMMA));
            }
        }

        return tokens;
    }

    public static List<Token> flatten(List<List<Token>> tokenLists) {
        return tokenLists.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static Token getToken(TokenType tokenType, Object value) {
        return new Token(tokenType, new Position(0, 0), value);
    }

    private static Token getToken(TokenType tokenType) {
        return getToken(tokenType, null);
    }

}
