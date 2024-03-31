package lexer;

import java.util.Map;
import java.util.Set;

public final class TokenTypeMapper {
    private TokenTypeMapper() {
    }

    public static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("if", TokenType.IF_KEYWORD),
            Map.entry("else", TokenType.ELSE_KEYWORD),
            Map.entry("while", TokenType.WHILE_KEYWORD),
            Map.entry("foreach", TokenType.FOREACH_KEYWORD),
            Map.entry("return", TokenType.RETURN_KEYWORD),
            Map.entry("as", TokenType.AS_KEYWORD),
            Map.entry("void", TokenType.VOID_KEYWORD),
            Map.entry("int", TokenType.INT_KEYWORD),
            Map.entry("float", TokenType.FLOAT_KEYWORD),
            Map.entry("string", TokenType.STRING_KEYWORD),
            Map.entry("dict", TokenType.DICT_KEYWORD),
            Map.entry("null", TokenType.NULL_KEYWORD)
    );

    public static final Map<String, TokenType> SINGLE_SIGN_OPERATORS = Map.ofEntries(
            Map.entry("(", TokenType.LEFT_ROUND_BRACKET),
            Map.entry(")", TokenType.RIGHT_ROUND_BRACKET),
            Map.entry("[", TokenType.LEFT_SQUARE_BRACKET),
            Map.entry("]", TokenType.RIGHT_SQUARE_BRACKET),
            Map.entry("{", TokenType.LEFT_CURLY_BRACKET),
            Map.entry("}", TokenType.RIGHT_CURLY_BRACKET),
            Map.entry(",", TokenType.COMMA),
            Map.entry(":", TokenType.COLON),
            Map.entry(";", TokenType.SEMICOLON),
            Map.entry("+", TokenType.PLUS_OPERATOR),
            Map.entry("-", TokenType.MINUS_OPERATOR),
            Map.entry("*", TokenType.MULTIPLICATION_OPERATOR),
            Map.entry("/", TokenType.DIVISION_OPERATOR),
            Map.entry("%", TokenType.MODULO_OPERATOR),
            Map.entry("?", TokenType.NULLABLE_OPERATOR),
            Map.entry("<", TokenType.LESS_THAN_OPERATOR),
            Map.entry(">", TokenType.GREATER_THAN_OPERATOR),
            Map.entry("!", TokenType.NEGATION_OPERATOR),
            Map.entry("=", TokenType.ASSIGNMENT)
    );

    public static final Map<String, TokenType> DOUBLE_SIGN_OPERATORS = Map.of(
            "<=", TokenType.LESS_THAN_OR_EQUAL_OPERATOR,
            ">=", TokenType.GREATER_THAN_OR_EQUAL_OPERATOR,
            "==", TokenType.EQUAL_OPERATOR,
            "!=", TokenType.NOT_EQUAL_OPERATOR,
            "||", TokenType.OR_OPERATOR,
            "&&", TokenType.AND_OPERATOR
    );

    public static final Set<String> SIGNS_THAT_MIGHT_LEAD_TO_DOUBLE_SIGN_OPERATOR = Set.of("=", "<", ">", "!", "&", "|");
}
