package parser;

import ast.expression.DivideExpression;
import ast.expression.Equal;
import ast.expression.Expression;
import ast.expression.GreaterThan;
import ast.expression.GreaterThanOrEqual;
import ast.expression.LessThan;
import ast.expression.LessThanOrEqual;
import ast.expression.MinusExpression;
import ast.expression.ModuloExpression;
import ast.expression.MultiplyExpression;
import ast.expression.NotEqual;
import ast.expression.PlusExpression;
import lexer.TokenType;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

final class OperatorFactory {
    private OperatorFactory() {}

    private static final Map<TokenType, BiFunction<Expression, Expression, Expression>> relationalOperators = Map.of(
            TokenType.LESS_THAN_OPERATOR, LessThan::new,
            TokenType.LESS_THAN_OR_EQUAL_OPERATOR, LessThanOrEqual::new,
            TokenType.GREATER_THAN_OPERATOR, GreaterThan::new,
            TokenType.GREATER_THAN_OR_EQUAL_OPERATOR, GreaterThanOrEqual::new,
            TokenType.EQUAL_OPERATOR, Equal::new,
            TokenType.NOT_EQUAL_OPERATOR, NotEqual::new
    );

    private static final Map<TokenType, BiFunction<Expression, Expression, Expression>> additiveOperators = Map.of(
            TokenType.PLUS_OPERATOR, PlusExpression::new,
            TokenType.MINUS_OPERATOR, MinusExpression::new
    );

    private static final Map<TokenType, BiFunction<Expression, Expression, Expression>> multiplicativeOperators = Map.of(
            TokenType.MULTIPLICATION_OPERATOR, MultiplyExpression::new,
            TokenType.DIVISION_OPERATOR, DivideExpression::new,
            TokenType.MODULO_OPERATOR, ModuloExpression::new
    );

    public static boolean isRelationalOperator(TokenType tokenType) {
        return relationalOperators.containsKey(tokenType);
    }


    public static boolean isAdditiveOperator(TokenType tokenType) {
        return additiveOperators.containsKey(tokenType);
    }

    public static boolean isMultiplicativeOperator(TokenType tokenType) {
        return multiplicativeOperators.containsKey(tokenType);
    }

    public static Expression createRelationalExpression(TokenType relationalOperator, Expression leftLogicFactor, Expression rightLogicFactor) {
        return createBinaryExpression(relationalOperators, relationalOperator, leftLogicFactor, rightLogicFactor);
    }

    public static Expression createAdditiveExpression(TokenType additiveOperator, Expression leftLogicFactor, Expression rightLogicFactor) {
        return createBinaryExpression(additiveOperators, additiveOperator, leftLogicFactor, rightLogicFactor);

    }

    public static Expression createMultiplicativeExpression(TokenType multiplicativeOperator, Expression leftLogicFactor, Expression rightLogicFactor) {
        return createBinaryExpression(multiplicativeOperators, multiplicativeOperator, leftLogicFactor, rightLogicFactor);

    }

    private static Expression createBinaryExpression(
            Map<TokenType, BiFunction<Expression, Expression, Expression>> operators,
            TokenType tokenType,
            Expression leftLogicFactor,
            Expression rightLogicFactor
    ) {
        return Optional.ofNullable(operators.get(tokenType))
                .map(op -> op.apply(leftLogicFactor, rightLogicFactor))
                .orElseThrow(() -> new IllegalArgumentException("Invalid operator token type"));
    }
}
