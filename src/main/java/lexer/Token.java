package lexer;


public record Token(TokenType type, Position position, Object value) {
    public static Token eof() {
        return new Token(TokenType.EOF, null, null);
    }
}
