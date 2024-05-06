package parser;

import lexer.Position;
import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static lexer.TokenType.COLON;
import static lexer.TokenType.COMMA;
import static lexer.TokenType.DICT_KEYWORD;
import static lexer.TokenType.ELSE_KEYWORD;
import static lexer.TokenType.FLOAT_KEYWORD;
import static lexer.TokenType.FOREACH_KEYWORD;
import static lexer.TokenType.IDENTIFIER;
import static lexer.TokenType.IF_KEYWORD;
import static lexer.TokenType.LEFT_CURLY_BRACKET;
import static lexer.TokenType.LEFT_ROUND_BRACKET;
import static lexer.TokenType.LEFT_SQUARE_BRACKET;
import static lexer.TokenType.RETURN_KEYWORD;
import static lexer.TokenType.RIGHT_CURLY_BRACKET;
import static lexer.TokenType.RIGHT_ROUND_BRACKET;
import static lexer.TokenType.RIGHT_SQUARE_BRACKET;
import static lexer.TokenType.SEMICOLON;
import static lexer.TokenType.WHILE_KEYWORD;

public class TokenFactory {
    private TokenFactory() {
    }

    public static List<Token> program(List<List<Token>> tokenLists) {
        var tokens = flatten(tokenLists);
        tokens.add(Token.eof());

        return tokens;
    }

    public static List<Token> function(TokenType returnType, String name, List<Token> params, List<Token> content) {
        return function(List.of(returnType), name, params, content);
    }

    public static List<Token> function(List<TokenType> returnType, String name, List<Token> params, List<Token> content) {
        List<Token> tokens = new ArrayList<>();
        returnType.forEach(t -> tokens.add(getToken(t)));

        tokens.add(getToken(IDENTIFIER, name));
        tokens.add(getToken(LEFT_ROUND_BRACKET));
        tokens.addAll(params);
        tokens.add(getToken(RIGHT_ROUND_BRACKET));
        tokens.add(getToken(LEFT_CURLY_BRACKET));
        tokens.addAll(content);
        tokens.add(getToken(RIGHT_CURLY_BRACKET));

        return tokens;
    }

    public static List<TokenType> dict(TokenType typeParam1, TokenType typeParam2) {
        var tokens = new ArrayList<>(List.of(DICT_KEYWORD, LEFT_SQUARE_BRACKET, FLOAT_KEYWORD, COMMA, FLOAT_KEYWORD, RIGHT_SQUARE_BRACKET));

        tokens.set(2, typeParam1);
        tokens.set(4, typeParam2);

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

    public static List<Token> returnStatement(List<Token> returnExpression) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(getToken(RETURN_KEYWORD));
        tokens.addAll(returnExpression);
        tokens.add(getToken(SEMICOLON));
        return tokens;
    }

    public static List<Token> whileStatement(List<Token> condition, List<Token> body) {
        return Stream.of(
                List.of(getToken(WHILE_KEYWORD), getToken(LEFT_ROUND_BRACKET)),
                condition,
                List.of(getToken(RIGHT_ROUND_BRACKET), getToken(LEFT_CURLY_BRACKET)),
                body,
                List.of(getToken(RIGHT_CURLY_BRACKET))
        ).flatMap(Collection::stream).toList();
    }

    public static List<Token> foreachStatement(TokenType type, String name, List<Token> iterable, List<Token> body) {
        return Stream.of(
                List.of(getToken(FOREACH_KEYWORD), getToken(LEFT_ROUND_BRACKET),
                        getToken(type), getToken(IDENTIFIER, name), getToken(COLON)),
                iterable,
                List.of(getToken(RIGHT_ROUND_BRACKET), getToken(LEFT_CURLY_BRACKET)),
                body,
                List.of(getToken(RIGHT_CURLY_BRACKET))
        ).flatMap(Collection::stream).toList();
    }

    public static List<Token> ifElseStatement(List<Token> condition, List<Token> ifBody, List<Token> elseBody) {
        return Stream.of(
                List.of(getToken(IF_KEYWORD), getToken(LEFT_ROUND_BRACKET)),
                condition,
                List.of(getToken(RIGHT_ROUND_BRACKET), getToken(LEFT_CURLY_BRACKET)),
                ifBody,
                List.of(getToken(RIGHT_CURLY_BRACKET), getToken(ELSE_KEYWORD), getToken(LEFT_CURLY_BRACKET)),
                elseBody,
                List.of(getToken(RIGHT_CURLY_BRACKET))
        ).flatMap(Collection::stream).toList();
    }

    public static List<Token> flatten(List<List<Token>> tokenLists) {
        return tokenLists.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static Token getToken(TokenType tokenType, Object value) {
        return new Token(tokenType, new Position(0, 0), value);
    }

    public static Token getToken(TokenType tokenType) {
        return getToken(tokenType, null);
    }

}
