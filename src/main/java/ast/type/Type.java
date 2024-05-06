package ast.type;

import lexer.TokenType;

import java.util.Objects;

public interface Type {
    static SimpleType simpleType(TokenType tokenType) {
        switch (tokenType) {
            case INT_KEYWORD -> {
                return new IntType();
            }
            case FLOAT_KEYWORD -> {
                return new FloatType();
            }
            case STRING_KEYWORD -> {
                return new StringType();
            }
            default -> throw new IllegalStateException(String.format("%s is not a valid simple type", tokenType));
        }
    }

    static CollectionType collectionType(TokenType tokenType, SimpleType ...simpleTypes) {
        if (Objects.requireNonNull(tokenType) == TokenType.DICT_KEYWORD) {
            return new DictType(simpleTypes[0], simpleTypes[1]);
        }
        throw new IllegalStateException(String.format("%s is not a valid collection type", tokenType));
    }

}
