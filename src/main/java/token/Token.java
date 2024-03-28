package token;

import token.TokenPosition;
import token.TokenType;

public record Token(TokenType tokenType, TokenPosition position, Object value) {
}
