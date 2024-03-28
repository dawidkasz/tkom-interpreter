package lexer;

import token.Token;
import token.TokenPosition;
import token.TokenType;

import java.util.Optional;

import static token.TokenTypeMapper.DOUBLE_SIGN_OPERATORS;
import static token.TokenTypeMapper.KEYWORDS;
import static token.TokenTypeMapper.SIGNS;
import static token.TokenTypeMapper.SIMPLE_SIGNS;
import static token.TokenTypeMapper.SINGLE_SIGN_OPERATORS;

public class Lexer {

    private final CharacterProvider characterProvider;
    private int currentLine = 1;
    private int currentColumn = 1;
    private Character currentCharacter;
    private boolean isEndOfFile = false;

    public Lexer(CharacterProvider characterProvider) {
        this.characterProvider = characterProvider;
        this.currentCharacter = characterProvider.next();
    }

    public Token nextToken() {
        while (!isEndOfFile && Character.isWhitespace(currentCharacter)) {
            getNextCharacterAndIncrementPosition();
        }

        if (isEndOfFile) {
            return new Token(TokenType.EOF, new TokenPosition(currentLine, currentCharacter), null);
        }

        if (Character.isLetter(currentCharacter) || currentCharacter == '_') {
            return processKeywordOrIdentifier();
        } else if (Character.isDigit(currentCharacter)) {
            return processNumberLiteral();
        } else if (SIMPLE_SIGNS.containsKey(String.valueOf(currentCharacter)) || SIGNS.contains(String.valueOf(currentCharacter))) {
            return processSpecialCharacter();
        }

        throw new RuntimeException("Unrecognized token");
    }

    private void getNextCharacterAndIncrementPosition() {
        if (!characterProvider.hasNext()) {
            isEndOfFile = true;
            return;
        }

        if (currentCharacter == '\n') {
            currentLine++;
            currentColumn = 1;
        } else {
            currentColumn++;
        }

        currentCharacter = characterProvider.next();
    }

    private Token processKeywordOrIdentifier() {
        var buffer = new StringBuilder();
        var position = new TokenPosition(currentLine, currentColumn);

        while (Character.isLetter(currentCharacter) || Character.isDigit(currentCharacter) || currentCharacter == '_') {
            buffer.append(currentCharacter);
            Optional<TokenType> keywordType = asKeywordTokenType(buffer.toString());

            boolean hasNext = characterProvider.hasNext();

            getNextCharacterAndIncrementPosition();

            if (keywordType.isPresent()) {
                return new Token(keywordType.get(), position, buffer.toString());
            }

            if (!hasNext) {
                break;
            }
        }

        return new Token(TokenType.IDENTIFIER, position, buffer.toString());
    }

    public Token processNumberLiteral() {
        int number = 0;
        var position = new TokenPosition(currentLine, currentColumn);

        while (Character.isDigit(currentCharacter)) {
            number = number * 10 + Character.getNumericValue(currentCharacter);

            boolean hasNext = characterProvider.hasNext();

            getNextCharacterAndIncrementPosition();

            if (!hasNext) {
                break;
            }
        }

        return new Token(TokenType.INT_LITERAL, position, number);
    }

    public Token processSpecialCharacter() {
        String firstChar = String.valueOf(currentCharacter);
        var position = new TokenPosition(currentLine, currentColumn);

        if (SIMPLE_SIGNS.containsKey(firstChar)) {
            getNextCharacterAndIncrementPosition();
            return new Token(SIMPLE_SIGNS.get(firstChar), position, firstChar);
        }

        getNextCharacterAndIncrementPosition();

        String secondChar = String.valueOf(currentCharacter);
        String combined = firstChar + secondChar;

        if (DOUBLE_SIGN_OPERATORS.containsKey(combined)) {
            getNextCharacterAndIncrementPosition();

            return new Token(DOUBLE_SIGN_OPERATORS.get(combined), position, combined);
        }

        if (SINGLE_SIGN_OPERATORS.containsKey(firstChar)) {
            return new Token(SINGLE_SIGN_OPERATORS.get(firstChar), position, firstChar);
        }

        throw new RuntimeException("Unrecognized special character");
    }

    private Optional<TokenType> asKeywordTokenType(String value) {
        return Optional.ofNullable(KEYWORDS.get(value));
    }
}
