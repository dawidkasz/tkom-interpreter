package parser;

import ast.FunctionDefinition;
import ast.Parameter;
import ast.Program;
import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DivideExpression;
import ast.expression.Expression;
import ast.expression.FloatLiteral;
import ast.expression.IntLiteral;
import ast.expression.LessThanExpression;
import ast.expression.MinusExpression;
import ast.expression.ModuloExpression;
import ast.expression.MultiplyExpression;
import ast.expression.NegatedExpression;
import ast.expression.NullableExpression;
import ast.expression.OrExpression;
import ast.expression.PlusExpression;
import ast.expression.StringLiteral;
import ast.statement.ReturnStatement;
import ast.statement.Statement;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DefaultParser implements Parser {
    private final Lexer lexer;

    private Token token;

    public DefaultParser(Lexer lexer) {
        this.lexer = lexer;
        consumeToken();
    }

    // program = {variableDeclaration | functionDefinition};
    @Override
    public Program parseProgram() {
        Map<String, FunctionDefinition> functions = new HashMap<>();

        var fun = this.parseFunctionDefinition();
        while (fun.isPresent()) {
            functions.put(fun.get().getName(), fun.get());
            fun = this.parseFunctionDefinition();
        }

        return new Program(functions);
    }

    // functionDefinition = functionReturnType identifier parameterList statementBlock;
    private Optional<FunctionDefinition> parseFunctionDefinition() {
        if (!isFunctionReturnType(token.type())) {
            return Optional.empty();
        }

        TokenType type = token.type();

        consumeToken();
        if (token.type() != TokenType.IDENTIFIER) {
            throw new SyntaxError("Expected identifer");
        }
        var name = (String) token.value();

        consumeToken();
        if (token.type() != TokenType.LEFT_ROUND_BRACKET) {
            throw new SyntaxError("Expected left parentheses");
        }

        consumeToken();
        var params = parseParameters();

        if (token.type() != TokenType.RIGHT_ROUND_BRACKET) {
            throw new SyntaxError(String.format("Expected right parentheses, but received %s", token.type()));
        }

        consumeToken();
        var block = parseStatementBlock();

        return Optional.of(new FunctionDefinition(type.name(), name, params, block));
    }

    // parameters = [parameter, {"," parameter}];
    private List<Parameter> parseParameters() {
        List<Parameter> parameters = new ArrayList<>();

        var parameter = parseParameter();
        if (parameter.isEmpty()) {
            return parameters;
        }

        parameters.add(parameter.get());

        consumeToken();

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            parameter = parseParameter();
            if (parameter.isEmpty()) {
                throw new SyntaxError("Expected parameter");
            }
            parameters.add(parameter.get());
            consumeToken();
        }

        return parameters;
    }

    // parameter = type identifier;
    private Optional<Parameter> parseParameter() {
        var type = parseType();
        if (type.isEmpty()) {
            return Optional.empty();
        }

        consumeToken();
        if (token.type() != TokenType.IDENTIFIER) {
            throw new SyntaxError("Expected identifier");
        }

        var identifier = (String) token.value();

        return Optional.of(new Parameter(type.get(), identifier));
    }

    // type = simpleType | parametrizedType;
    private Optional<String> parseType() {
        if (isSimpleType(token.type())) {
            return Optional.of(token.type().name());
        }

        if (isCollectionType(token.type())) {
            var type = token.type();

            consumeToken();
            if (token.type() != TokenType.LEFT_SQUARE_BRACKET) {
                throw new SyntaxError("Expected left square bracket");
            }

            consumeToken();
            if (!isSimpleType(token.type())) {
                throw new SyntaxError("Expected simple type");
            }
            var t1 = token.type().name();

            consumeToken();
            if (token.type() != TokenType.COMMA) {
                throw new SyntaxError("Expected comma");
            }

            consumeToken();
            if (!isSimpleType(token.type())) {
                throw new SyntaxError("Expected simple type");
            }
            var t2 = token.type().name();

            consumeToken();
            if (token.type() != TokenType.RIGHT_SQUARE_BRACKET) {
                throw new SyntaxError("Expected right square bracket");
            }

            return Optional.of(String.format("%s[%s %s]", type, t1, t2));
        }

        return Optional.empty();
    }

    // statementBlock = "{" {statement} "}";
    private List<Statement> parseStatementBlock() {
        if (token.type() != TokenType.LEFT_CURLY_BRACKET) {
            throw new SyntaxError("Expected left curly bracket");
        }

        consumeToken();

        List<Statement> statements = new ArrayList<>();

        var statement = parseStatement();
        while (statement.isPresent()) {
            statements.add(statement.get());
            statement = parseStatement();
        }

        if (token.type() != TokenType.RIGHT_CURLY_BRACKET) {
            System.out.println(token);
            throw new SyntaxError("Expected right curly bracket");
        }

        consumeToken();

        return statements;
    }

    // statement = ifStatement | whileStatement | forEachStatement | variableDeclaration | assignment | functionCall | returnStatement;
    private Optional<Statement> parseStatement() {
        return parseReturnStatement();
    }

    private Optional<Statement> parseReturnStatement() {
        if (token.type() != TokenType.RETURN_KEYWORD) {
            return Optional.empty();
        }

        consumeToken();

        Optional<Expression> returnExpression = parseExpression();
        if (returnExpression.isEmpty()) {
            throw new SyntaxError("Missing return expression");
        }

        return Optional.of(new ReturnStatement(returnExpression.get()));
    }

    // expression = andExpression {orOperator andExpression};
    private Optional<Expression> parseExpression() {
        var left = parseAndExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        while (token.type() == TokenType.OR_OPERATOR) {
            consumeToken();
            var rightLogicFactor = parseAndExpression();
            if (rightLogicFactor.isEmpty()) {
                throw new SyntaxError("Missing right side of or operator");
            }

            leftLogicFactor = new OrExpression(leftLogicFactor, rightLogicFactor.get());
        }

        return Optional.of(leftLogicFactor);
    }

    // andExpression = relationExpression {andOperator relationExpression};
    private Optional<Expression> parseAndExpression() {
        var left = parseRelationExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        while (token.type() == TokenType.AND_OPERATOR) {
            consumeToken();
            var rightLogicFactor = parseRelationExpression();
            if (rightLogicFactor.isEmpty()) {
                throw new SyntaxError("Missing right side of && operator");
            }

            leftLogicFactor = new AndExpression(leftLogicFactor, rightLogicFactor.get());
        }

        return Optional.of(leftLogicFactor);
    }

    // relationExpression = additiveExpression [relationOperator additiveExpression];
    private Optional<Expression> parseRelationExpression() {
        var left = parseAdditiveExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        if (!Set.of(TokenType.LESS_THAN_OPERATOR, TokenType.LESS_THAN_OR_EQUAL_OPERATOR, TokenType.GREATER_THAN_OPERATOR,
                TokenType.GREATER_THAN_OR_EQUAL_OPERATOR, TokenType.EQUAL_OPERATOR, TokenType.NOT_EQUAL_OPERATOR).contains(token.type())) {
            return left;
        }

        consumeToken();

        var rightLogicFactor = parseAdditiveExpression();
        if (rightLogicFactor.isEmpty()) {
            throw new SyntaxError("Missing right side of < operator");
        }

        return Optional.of(new LessThanExpression(leftLogicFactor, rightLogicFactor.get()));
    }

    private Optional<Expression> parseAdditiveExpression() {
        var left = parseMultiplicativeExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        boolean plus = false;
        while (token.type() == TokenType.PLUS_OPERATOR || token.type() == TokenType.MINUS_OPERATOR) {
            plus = token.type() == TokenType.PLUS_OPERATOR;
            consumeToken();
            var rightLogicFactor = parseMultiplicativeExpression();
            if (rightLogicFactor.isEmpty()) {
                throw new SyntaxError("Missing right side of + operator");
            }

            if (plus) {
                leftLogicFactor = new PlusExpression(leftLogicFactor, rightLogicFactor.get());
            } else {
                leftLogicFactor = new MinusExpression(leftLogicFactor, rightLogicFactor.get());
            }
        }

        return Optional.of(leftLogicFactor);
    }

    private Optional<Expression> parseMultiplicativeExpression() {
        var left = parseNullableExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        TokenType type;
        while (
            token.type() == TokenType.MULTIPLICATION_OPERATOR ||
            token.type() == TokenType.DIVISION_OPERATOR ||
            token.type() == TokenType.MODULO_OPERATOR
        ) {
            type = token.type();

            consumeToken();
            var rightLogicFactor = parseNullableExpression();
            if (rightLogicFactor.isEmpty()) {
                throw new SyntaxError("Missing right side of * operator");
            }

            if (type == TokenType.MULTIPLICATION_OPERATOR) {
                leftLogicFactor = new MultiplyExpression(leftLogicFactor, rightLogicFactor.get());
            } else if (type == TokenType.DIVISION_OPERATOR ) {
                leftLogicFactor = new DivideExpression(leftLogicFactor, rightLogicFactor.get());
            } else {
                leftLogicFactor = new ModuloExpression(leftLogicFactor, rightLogicFactor.get());
            }
        }

        return Optional.of(leftLogicFactor);
    }

    // nullableSingleExpression = negatedSingleExpression ["?"];
    private Optional<Expression> parseNullableExpression() {
        var expression = parseNegatedExpression();
        if (expression.isEmpty()) {
            return expression;
        }

        if (token.type() == TokenType.NULLABLE_OPERATOR) {
            consumeToken();
            return Optional.of(new NullableExpression(expression.get()));
        }

        return expression;
    }

    // negatedSingleExpression = ["!" | "-"] castedExpression;
    private Optional<Expression> parseNegatedExpression() {
        if (token.type() == TokenType.NEGATION_OPERATOR || token.type() == TokenType.MINUS_OPERATOR) {
            consumeToken();
            var expression = parseCastedExpression();
            if (expression.isEmpty()) {
                throw new SyntaxError("Expected expression after negation operator");
            }

            return Optional.of(new NegatedExpression(expression.get()));
        }

        return parseCastedExpression();
    }

    // castedExpression = singleExpression ["as" simpleType];
    private Optional<Expression> parseCastedExpression() {
        var expression = parseSimpleExpression();
        if (expression.isEmpty()) {
            return expression;
        }

        if (token.type() == TokenType.AS_KEYWORD) {
            consumeToken();

            if (!isType(token.type())) {
                throw new SyntaxError("Expected type after as keyword");
            }
            String type = token.type().name();
            consumeToken();
            return Optional.of(new CastedExpression(expression.get(), type));
        }

        return expression;
    }

    private Optional<Expression> parseSimpleExpression() {
        if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
            consumeToken();

            var expression = parseExpression();
            if (expression.isEmpty()) {
                throw new SyntaxError("Missing expression inside brackets");
            }

            if (token.type() != TokenType.RIGHT_ROUND_BRACKET) {
                throw new SyntaxError("Missing right round bracket");
            }

            consumeToken();

            return expression;
        }

        if (token.type() == TokenType.INT_LITERAL) {
            int value = (int) token.value();
            consumeToken();
            return Optional.of(new IntLiteral(value));
        }

        if (token.type() == TokenType.FLOAT_LITERAL) {
            float value = (float) token.value();
            consumeToken();
            return Optional.of(new FloatLiteral(value));
        }

        if (token.type() == TokenType.STRING_LITERAL) {
            String value = (String) token.value();
            consumeToken();
            return Optional.of(new StringLiteral(value));
        }

        if (token.type() == TokenType.IDENTIFIER) {
            var identifier = token;
            consumeToken();
            if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
                consumeToken();

            }
        }

        return Optional.empty();
    }

    // argumentList = [expression, {"," expression}];
    private List<Expression> parseArgumentList() {
        List<Expression> arguments = new ArrayList<>();

        var argument = parseExpression();
        if (argument.isEmpty()) {
            return arguments;
        }

        arguments.add(argument.get());

        consumeToken();

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            argument = parseExpression();
            if (argument.isEmpty()) {
                throw new SyntaxError("Expected argument after comma");
            }
            arguments.add(argument.get());
            consumeToken();
        }

        return arguments;

    }

    private void consumeToken() {
        this.token = lexer.nextToken();
    }

    private boolean isSimpleType(TokenType tokenType) {
        return tokenType == TokenType.INT_KEYWORD ||
                tokenType == TokenType.FLOAT_KEYWORD ||
                tokenType == TokenType.STRING_KEYWORD;
    }

    private boolean isCollectionType(TokenType tokenType) {
        return tokenType == TokenType.DICT_KEYWORD;
    }

    private boolean isType(TokenType tokenType) {
        return isSimpleType(tokenType) || isCollectionType(tokenType);
    }

    private boolean isFunctionReturnType(TokenType tokenType) {
        return isType(tokenType) || tokenType == TokenType.VOID_KEYWORD;
    }

    public static class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }
    }
}
