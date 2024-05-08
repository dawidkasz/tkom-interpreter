package parser;

import lexer.Lexer;
import lexer.Token;

import java.util.List;

public class MockLexer implements Lexer {
    private final List<Token> tokens;
    private int currentToken = 0;

    MockLexer(List<Token> tokens) {
        this.tokens = tokens;
    }

    @Override
    public Token nextToken() {
        return tokens.get(currentToken++);
    }
}
