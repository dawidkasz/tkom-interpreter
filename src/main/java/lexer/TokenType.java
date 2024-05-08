package lexer;

public enum TokenType {
    IF_KEYWORD,
    ELSE_KEYWORD,
    WHILE_KEYWORD,
    FOREACH_KEYWORD,
    RETURN_KEYWORD,
    AS_KEYWORD,
    VOID_KEYWORD,
    INT_KEYWORD,
    FLOAT_KEYWORD,
    STRING_KEYWORD,
    DICT_KEYWORD,
    NULL_KEYWORD,

    LEFT_ROUND_BRACKET,
    RIGHT_ROUND_BRACKET,
    LEFT_SQUARE_BRACKET,
    RIGHT_SQUARE_BRACKET,
    LEFT_CURLY_BRACKET,
    RIGHT_CURLY_BRACKET,

    COMMA,
    COLON,
    SEMICOLON,

    PLUS_OPERATOR,
    MINUS_OPERATOR,
    MULTIPLICATION_OPERATOR,
    DIVISION_OPERATOR,
    MODULO_OPERATOR,
    NEGATION_OPERATOR,
    NULLABLE_OPERATOR,

    OR_OPERATOR,
    AND_OPERATOR,
    LESS_THAN_OPERATOR,
    LESS_THAN_OR_EQUAL_OPERATOR,
    GREATER_THAN_OPERATOR,
    GREATER_THAN_OR_EQUAL_OPERATOR,
    EQUAL_OPERATOR,
    NOT_EQUAL_OPERATOR,

    ASSIGNMENT,
    IDENTIFIER,
    STRING_LITERAL,
    INT_LITERAL,
    FLOAT_LITERAL,
    EOF;

    public boolean isRelationalOperator() {
        return this == EQUAL_OPERATOR || this == NOT_EQUAL_OPERATOR || this == LESS_THAN_OPERATOR ||
                this == LESS_THAN_OR_EQUAL_OPERATOR || this == GREATER_THAN_OPERATOR || this == GREATER_THAN_OR_EQUAL_OPERATOR;
    }

    public boolean isSimpleType() {
        return this == INT_KEYWORD || this == FLOAT_KEYWORD || this == STRING_KEYWORD;
    }

    public boolean isCollectionType() {
        return this == DICT_KEYWORD;
    }

    public boolean isFunctionReturnType() {
        return isSimpleType() || isCollectionType() || this == VOID_KEYWORD;
    }
}
