package lexer;

import lexer.characterprovider.CharacterProvider;

import java.util.Optional;

import static lexer.TokenTypeMapper.DOUBLE_SIGN_OPERATORS;
import static lexer.TokenTypeMapper.KEYWORDS;
import static lexer.TokenTypeMapper.SIGNS_THAT_MIGHT_LEAD_TO_DOUBLE_SIGN_OPERATOR;
import static lexer.TokenTypeMapper.SINGLE_SIGN_OPERATORS;

public class Lexer {
    private static final int IDENTIFIER_LENGTH_LIMIT = 128;
    private static final int STRING_LITERAL_LENGTH_LIMIT = 1024;

    private final CharacterProvider characterProvider;
    private PositionedCharacter currentCharacter;
    private boolean isEndOfFile = false;

    public Lexer(CharacterProvider characterProvider) {
        this.characterProvider = characterProvider;
        readNextCharacter();
    }

    public Token nextToken() {
        skipWhiteCharacters();

        if (isEndOfFile) {
            return Token.eof(currentCharacter.position());
        }

        return processKeywordOrIdentifier()
                .or(this::processStringLiteral)
                .or(this::processNumberLiteral)
                .or(this::processSpecialCharacter)
                .orElseThrow(() -> new LexerException("Unrecognized token", currentCharacter.position()));
    }

    private void skipWhiteCharacters() {
        while (!isEndOfFile && Character.isWhitespace(currentCharacter.character())) {
            readNextCharacter();
        }
    }

    private Optional<Token> processKeywordOrIdentifier() {
        if (!Character.isLetter(currentCharacter.character()) && currentCharacter.character() != '_') {
            return Optional.empty();
        }

        var buffer = new StringBuilder();
        Position position = currentCharacter.position();

        while (
            !isEndOfFile &&
            (Character.isLetter(currentCharacter.character()) || Character.isDigit(currentCharacter.character()) || currentCharacter.character() == '_')
        ) {
            buffer.append(currentCharacter.character());
            if (buffer.length() > IDENTIFIER_LENGTH_LIMIT) {
                throw new LexerException(String.format("Identifier length exceeded the limit of %s characters", IDENTIFIER_LENGTH_LIMIT), position);
            }

            readNextCharacter();
        }

        String constructedTokenValue = buffer.toString();

        return mapValueToKeywordTokenType(constructedTokenValue)
                .map(keywordTokenType -> new Token(keywordTokenType, position, constructedTokenValue))
                .or(() -> Optional.of(new Token(TokenType.IDENTIFIER, position, constructedTokenValue)));
    }

    private Optional<Token> processStringLiteral() {
        if (currentCharacter.character() != '"') {
            return Optional.empty();
        }

        var buffer = new StringBuilder();
        Position position = currentCharacter.position();

        readNextCharacter();

        boolean escapeNextChar = false;

        while (!isEndOfFile) {
            if (escapeNextChar) {
                escapeNextChar = false;
                buffer.append(escapeCharacterInStringLiteral(currentCharacter));
            } else if (currentCharacter.character() == '\\') {
                escapeNextChar = true;
            } else {
                buffer.append(currentCharacter.character());
            }

            if (buffer.length() > STRING_LITERAL_LENGTH_LIMIT) {
                throw new LexerException(String.format("String literal length exceeded the limit of %s characters", STRING_LITERAL_LENGTH_LIMIT), position);
            }

            readNextCharacter();

            if (currentCharacter.character() == '"' && !escapeNextChar) {
                readNextCharacter();
                break;
            }
        }

        return Optional.of(new Token(TokenType.STRING_LITERAL, position, buffer.toString()));
    }

    private Character escapeCharacterInStringLiteral(PositionedCharacter c) {
        return switch (c.character()) {
            case '\\' -> '\\';
            case '"' -> '"';
            case 'n' -> '\n';
            case 't' -> '\t';
            default -> throw new LexerException(String.format("Illegal escape character '\\%s'", c), c.position());
        };
    }

    private Optional<Token> processNumberLiteral() {
        if (!Character.isDigit(currentCharacter.character())) {
            return Optional.empty();
        }

        int decimalPart = 0;
        Position position = currentCharacter.position();

        if (currentCharacter.character() == '0') {
            readNextCharacter();

            if (!isEndOfFile && Character.isDigit(currentCharacter.character())) {
                throw new LexerException("Leading zeros in number literal", position);
            }
        } else {
            while (!isEndOfFile && Character.isDigit(currentCharacter.character())) {
                try {
                    decimalPart = Math.addExact(Math.multiplyExact(decimalPart, 10), Character.getNumericValue(currentCharacter.character()));
                } catch (ArithmeticException e) {
                    throw new LexerException("Overflow: number literal exceeded its maximum value", position);
                }
                readNextCharacter();
            }
        }

        if (currentCharacter.character() == '.') {
            readNextCharacter();
            float literalValue = decimalPart + processFractionalNumberLiteralPart(position);
            return Optional.of(new Token(TokenType.FLOAT_LITERAL, position, literalValue));
        }

        return Optional.of(new Token(TokenType.INT_LITERAL, position, decimalPart));
    }

    private float processFractionalNumberLiteralPart(Position literalBeginning) {
        if (isEndOfFile || !Character.isDigit(currentCharacter.character())) {
            throw new LexerException("Missing fractional part in a float literal", literalBeginning);
        }

        float fraction = 0f;
        float base = 0.1f;

        while (!isEndOfFile && Character.isDigit(currentCharacter.character())) {
            fraction = fraction + (float) Character.getNumericValue(currentCharacter.character()) * base;
            base *= 0.1f;

            readNextCharacter();
        }

        return fraction;
    }

    private Optional<Token> processSpecialCharacter() {
        String firstChar = String.valueOf(currentCharacter);
        Position position = currentCharacter.position();

        if (!SINGLE_SIGN_OPERATORS.containsKey(firstChar) && !SIGNS_THAT_MIGHT_LEAD_TO_DOUBLE_SIGN_OPERATOR.contains(firstChar)) {
            return Optional.empty();
        }

        boolean secondCharacterRead = false;

        if (characterProvider.hasNext()) {
            secondCharacterRead = true;
            readNextCharacter();
            String doubleSignOperator = firstChar + currentCharacter;

            TokenType doubleSignOperatorTokenType = DOUBLE_SIGN_OPERATORS.get(doubleSignOperator);
            if (doubleSignOperatorTokenType != null) {
                readNextCharacter();
                return Optional.of(new Token(doubleSignOperatorTokenType, position, doubleSignOperator));
            }
        }

        TokenType singleSignOperatorTokenType = SINGLE_SIGN_OPERATORS.get(firstChar);
        if (singleSignOperatorTokenType != null) {
            if (!secondCharacterRead) {
                readNextCharacter();
            }
            return Optional.of(new Token(singleSignOperatorTokenType, position, firstChar));
        }

        throw new LexerException("Invalid operator", position);
    }

    private void readNextCharacter() {
        if (!characterProvider.hasNext()) {
            isEndOfFile = true;
            return;
        }

        currentCharacter = characterProvider.next();
    }

    private Optional<TokenType> mapValueToKeywordTokenType(String value) {
        return Optional.ofNullable(KEYWORDS.get(value));
    }

    public static class LexerException extends RuntimeException {
        LexerException(String message, Position position) {
            super(String.format("%s (line=%s, column=%s)", message, position.lineNumber(), position.columnNumber()));
        }
    }
}
