package lexer;


public record Token(TokenType tokenType, Position position, Object value) {
    public static Token eof() {
        return new Token(TokenType.EOF, null, null);
    }
}
