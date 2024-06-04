package lexer;

import lexer.characterprovider.CharacterProvider;

import java.util.Optional;

import static lexer.TokenTypeMapper.DOUBLE_SIGN_OPERATORS;
import static lexer.TokenTypeMapper.KEYWORDS;
import static lexer.TokenTypeMapper.SIGNS_THAT_MIGHT_LEAD_TO_DOUBLE_SIGN_OPERATOR;
import static lexer.TokenTypeMapper.SINGLE_SIGN_OPERATORS;

public class DefaultLexer implements Lexer {
    private static final int DEFAULT_IDENTIFIER_LENGTH_LIMIT = 128;
    private static final int DEFAULT_STRING_LITERAL_LENGTH_LIMIT = 1024;
    private static final char BEGIN_COMMENT_CHARACTER = '#';
    private static final PositionedCharacter EOF = new PositionedCharacter('\uFFFF', null);

    private final CharacterProvider characterProvider;
    private final int identifierLengthLimit;
    private final int stringLiteralLengthLimit;

    private PositionedCharacter currentCharacter;

    public DefaultLexer(CharacterProvider characterProvider, int identifierLengthLimit, int stringLiteralLengthLimit) {
        this.characterProvider = characterProvider;
        this.identifierLengthLimit = identifierLengthLimit;
        this.stringLiteralLengthLimit = stringLiteralLengthLimit;
        readNextCharacter();
    }

    public DefaultLexer(CharacterProvider characterProvider) {
        this(characterProvider, DEFAULT_IDENTIFIER_LENGTH_LIMIT, DEFAULT_STRING_LITERAL_LENGTH_LIMIT);
    }

    @Override
    public Token nextToken() {
        skipWhiteCharactersAndComments();

        if (currentCharacter == EOF) {
            return Token.eof();
        }

        return processKeywordOrIdentifier()
                .or(this::processStringLiteral)
                .or(this::processNumberLiteral)
                .or(this::processSpecialCharacter)
                .orElseThrow(() -> new LexerException("Unrecognized token", currentCharacter.position()));
    }

    private void skipWhiteCharactersAndComments() {
        while (
                Character.isWhitespace(currentCharacter.character()) ||
                currentCharacter.character() == BEGIN_COMMENT_CHARACTER
        ) {
            if (currentCharacter.character() == BEGIN_COMMENT_CHARACTER) {
                while(currentCharacter.character() != '\n') {
                    readNextCharacter();
                }
            } else {
                readNextCharacter();
            }
        }
    }

    private Optional<Token> processKeywordOrIdentifier() {
        if (!Character.isLetter(currentCharacter.character()) && currentCharacter.character() != '_') {
            return Optional.empty();
        }

        var buffer = new StringBuilder();
        Position position = currentCharacter.position();

        while (
            Character.isLetter(currentCharacter.character()) ||
            Character.isDigit(currentCharacter.character()) ||
            currentCharacter.character() == '_'
        ) {
            if (buffer.length() == identifierLengthLimit) {
                throw new LexerException(String.format("Identifier length exceeded the limit of %s characters", identifierLengthLimit), position);
            }

            buffer.append(currentCharacter.character());
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

        while (currentCharacter.character() != '"') {
            if (buffer.length() == stringLiteralLengthLimit) {
                throw new LexerException(String.format("String literal length exceeded the limit of %s characters", stringLiteralLengthLimit), position);
            }

            if (currentCharacter.character() == '\\') {
                readNextCharacter();
                buffer.append(mapEscapedCharacter(currentCharacter));
            } else {
                buffer.append(currentCharacter.character());
            }

            readNextCharacter();
        }

        readNextCharacter();

        return Optional.of(new Token(TokenType.STRING_LITERAL, position, buffer.toString()));
    }

    private Character mapEscapedCharacter(PositionedCharacter c) {
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

        Position position = currentCharacter.position();
        int decimalPart = processDecimalNumberLiteralPart(position);

        if (currentCharacter.character() == '.') {
            readNextCharacter();
            float literalValue = decimalPart + processFractionalNumberLiteralPart(position);
            return Optional.of(new Token(TokenType.FLOAT_LITERAL, position, literalValue));
        }

        return Optional.of(new Token(TokenType.INT_LITERAL, position, decimalPart));
    }

    private int processDecimalNumberLiteralPart(Position literalBeginning) {
        int decimalPart = 0;

        if (currentCharacter.character() == '0') {
            readNextCharacter();
            if (Character.isDigit(currentCharacter.character())) {
                throw new LexerException("Leading zero in number literal", literalBeginning);
            }

            return decimalPart;
        }

        while (Character.isDigit(currentCharacter.character())) {
            try {
                decimalPart = Math.addExact(Math.multiplyExact(decimalPart, 10), Character.getNumericValue(currentCharacter.character()));
            } catch (ArithmeticException e) {
                throw LexerException.numberOverFlow(literalBeginning);
            }
            readNextCharacter();
        }

        return decimalPart;
    }

    private float processFractionalNumberLiteralPart(Position literalBeginning) {
        if (!Character.isDigit(currentCharacter.character())) {
            throw new LexerException("Missing fractional part in a float literal", literalBeginning);
        }

        int base = 0;
        int decimalValue = 0;

        while (Character.isDigit(currentCharacter.character())) {
            try {
                decimalValue = Math.addExact(Math.multiplyExact(decimalValue, 10), Character.getNumericValue(currentCharacter.character()));
                base++;
            } catch (ArithmeticException e) {
                throw LexerException.numberOverFlow(literalBeginning);
            }

            readNextCharacter();
        }

        return decimalValue / (float) Math.pow(10, base);
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
        currentCharacter = characterProvider.hasNext() ? characterProvider.next() : EOF;
    }

    private Optional<TokenType> mapValueToKeywordTokenType(String value) {
        return Optional.ofNullable(KEYWORDS.get(value));
    }

    public static class LexerException extends RuntimeException {
        LexerException(String message, Position position) {
            super(String.format("%s (line=%s, column=%s)", message, position.lineNumber(), position.columnNumber()));
        }

        public static LexerException numberOverFlow(Position position) {
            return new LexerException(String.format("Overflow: number literal exceeded its maximum value of %s", Integer.MAX_VALUE), position);
        }
    }
}
