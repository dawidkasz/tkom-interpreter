package lexer;

public record Token(TokenType tokenType, Position position, Object value) {
}
