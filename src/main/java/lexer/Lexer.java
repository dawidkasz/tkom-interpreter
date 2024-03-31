package lexer;

import java.util.Optional;

import static lexer.TokenTypeMapper.DOUBLE_SIGN_OPERATORS;
import static lexer.TokenTypeMapper.KEYWORDS;
import static lexer.TokenTypeMapper.SIGNS;
import static lexer.TokenTypeMapper.SINGLE_SIGN_OPERATORS;

public class Lexer {
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
            return new Token(TokenType.EOF, currentCharacter.position(), null);
        }

        return processKeywordOrIdentifier()
                .or(this::processStringLiteral)
                .or(this::processNumberLiteral)
                .or(this::processSpecialCharacter)
                .orElseThrow(() -> new RuntimeException("Unrecognized token"));
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
                !isEndOfFile && (
                    Character.isLetter(currentCharacter.character()) ||
                    Character.isDigit(currentCharacter.character()) ||
                    currentCharacter.character() == '_'
                )
        ) {
            buffer.append(currentCharacter.character());
            readNextCharacter();
        }

        return matchWithKeywordTokenType(buffer.toString())
                .map(keywordTokenType -> new Token(keywordTokenType, position, buffer.toString()))
                .or(() -> Optional.of(new Token(TokenType.IDENTIFIER, position, buffer.toString())));
    }

    private Optional<Token> processStringLiteral() {
        if (currentCharacter.character() != '"') {
            return Optional.empty();
        }

        readNextCharacter();

        var buffer = new StringBuilder();
        Position position = currentCharacter.position();

        boolean escapeNextChar = false;

        while (!isEndOfFile) {
            if (escapeNextChar) {
                escapeNextChar = false;
                buffer.append(escapeCharacterInStringLiteral(currentCharacter.character()));
            } else if (currentCharacter.character() == '\\') {
                escapeNextChar = true;
            } else {
                buffer.append(currentCharacter.character());
            }

            readNextCharacter();

            if (currentCharacter.character() == '"' && !escapeNextChar) {
                readNextCharacter();
                break;
            }
        }

        return Optional.of(new Token(TokenType.STRING_LITERAL, position, buffer.toString()));
    }

    private Character escapeCharacterInStringLiteral(Character c) {
        return switch (c) {
            case '\\' -> '\\';
            case '"' -> '"';
            case 'n' -> '\n';
            case 't' -> '\t';
            default -> throw new RuntimeException(String.format("Unknown character to escape: '%s'", c));
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
            if (!isEndOfFile && currentCharacter.character() == '.') {
                readNextCharacter();
                return Optional.of(new Token(TokenType.FLOAT_LITERAL, position, processFractionalNumberLiteralPart()));
            }
            if (!isEndOfFile && Character.isDigit(currentCharacter.character())) {
                throw new RuntimeException("can't process leading zeros");
            }
        }

        while (!isEndOfFile && Character.isDigit(currentCharacter.character())) {
            decimalPart = decimalPart * 10 + Character.getNumericValue(currentCharacter.character());

            readNextCharacter();

            if (currentCharacter.character() == '.') {
                readNextCharacter();
                float literalValue = decimalPart + processFractionalNumberLiteralPart();
                return Optional.of(new Token(TokenType.FLOAT_LITERAL, position, literalValue));
            }
        }

        return Optional.of(new Token(TokenType.INT_LITERAL, position, decimalPart));
    }

    private float processFractionalNumberLiteralPart() {
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

        if (!SINGLE_SIGN_OPERATORS.containsKey(firstChar) && !SIGNS.contains(firstChar)) {
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

        return Optional.empty();
    }

    private void readNextCharacter() {
        if (!characterProvider.hasNext()) {
            isEndOfFile = true;
            return;
        }

        currentCharacter = characterProvider.next();
    }

    private Optional<TokenType> matchWithKeywordTokenType(String value) {
        return Optional.ofNullable(KEYWORDS.get(value));
    }
}
