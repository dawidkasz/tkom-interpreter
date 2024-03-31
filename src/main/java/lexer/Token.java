package lexer;


public record Token(TokenType tokenType, Position position, Object value) {
    public static Token eof(Position position) {
        return new Token(TokenType.EOF, position, null);
    }
}
