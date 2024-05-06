package parser;

import ast.FunctionCall;
import ast.FunctionDefinition;
import ast.Parameter;
import ast.Program;
import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DictValue;
import ast.expression.DictLiteral;
import ast.expression.DivideExpression;
import ast.expression.Equal;
import ast.expression.Expression;
import ast.expression.FloatLiteral;
import ast.expression.GreaterThan;
import ast.expression.GreaterThanOrEqual;
import ast.expression.IntLiteral;
import ast.expression.LessThan;
import ast.expression.LessThanOrEqual;
import ast.expression.MinusExpression;
import ast.expression.ModuloExpression;
import ast.expression.MultiplyExpression;
import ast.expression.NegatedExpression;
import ast.expression.NotEqual;
import ast.expression.Null;
import ast.expression.NullableExpression;
import ast.expression.OrExpression;
import ast.expression.PlusExpression;
import ast.expression.StringLiteral;
import ast.expression.VariableValue;
import ast.statement.ReturnStatement;
import ast.statement.Statement;
import ast.statement.WhileStatement;
import ast.type.Type;
import ast.type.VoidType;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;

import java.util.AbstractMap;
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
            functions.put(fun.get().name(), fun.get());
            fun = this.parseFunctionDefinition();
        }

        return new Program(functions);
    }

    // functionDefinition = functionReturnType identifier parameterList statementBlock;
    private Optional<FunctionDefinition> parseFunctionDefinition() {
        if (!isFunctionReturnType(token.type())) {
            return Optional.empty();
        }

        Type type = parseType().orElseThrow(() -> new SyntaxError("Expected function return type"));

        Token t = expectToken(TokenType.IDENTIFIER, "Expected an identifier");
        var name = (String) t.value();

        expectToken(TokenType.LEFT_ROUND_BRACKET, "Expected left parentheses");

        var params = parseParameters();

        expectToken(TokenType.RIGHT_ROUND_BRACKET, String.format("Expected right parentheses, but received %s", token.type()));

        var block = parseStatementBlock();

        return Optional.of(new FunctionDefinition(type, name, params, block));
    }

    // parameters = [parameter, {"," parameter}];
    private List<Parameter> parseParameters() {
        List<Parameter> parameters = new ArrayList<>();

        var parameter = parseParameter();
        if (parameter.isEmpty()) {
            return parameters;
        }

        parameters.add(parameter.get());

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            parameter = parseParameter();
            if (parameter.isEmpty()) {
                throw new SyntaxError("Expected parameter");
            }
            parameters.add(parameter.get());
        }

        return parameters;
    }

    // parameter = returnType identifier;
    private Optional<Parameter> parseParameter() {
        var type = parseType();
        if (type.isEmpty()) {
            return Optional.empty();
        }

        var identifier = expectToken(TokenType.IDENTIFIER, "Expected identifier");

        return Optional.of(new Parameter(type.get(), (String) identifier.value()));
    }

    // returnType = simpleType | parametrizedType;
    private Optional<Type> parseType() {
        if (token.type() == TokenType.VOID_KEYWORD) {
            consumeToken();
            return Optional.of(new VoidType());
        }

        if (isSimpleType(token.type())) {
            var type = Type.simpleType(token.type());
            consumeToken();
            return Optional.of(type);
        }

        if (isCollectionType(token.type())) {
            var tokenType = token.type();

            consumeToken();

            expectToken(TokenType.LEFT_SQUARE_BRACKET, "Expected left square bracket");

            if (!isSimpleType(token.type())) {
                throw new SyntaxError("Expected simple returnType");
            }
            var paramType1 = Type.simpleType(token.type());

            consumeToken();

            expectToken(TokenType.COMMA, "Expected comma");

            if (!isSimpleType(token.type())) {
                throw new SyntaxError("Expected simple returnType");
            }
            var paramType2 = Type.simpleType(token.type());

            consumeToken();

            expectToken(TokenType.RIGHT_SQUARE_BRACKET, "Expected right square bracket");

            return Optional.of(Type.collectionType(tokenType, paramType1, paramType2));
        }

        return Optional.empty();
    }

    // statementBlock = "{" {statement} "}";
    private List<Statement> parseStatementBlock() {
        expectToken(TokenType.LEFT_CURLY_BRACKET, "Expected left curly bracket");

        List<Statement> statements = new ArrayList<>();

        var statement = parseStatement();
        while (statement.isPresent()) {
            statements.add(statement.get());
            statement = parseStatement();
        }

        expectToken(TokenType.RIGHT_CURLY_BRACKET, "Expected right curly bracket");

        return statements;
    }

    // statement = ifStatement | whileStatement | forEachStatement | variableDeclaration | assignment | functionCall | returnStatement;
    private Optional<Statement> parseStatement() {
        return parseReturnStatement()
                .or(this::parseWhileStatement);
    }

    // returnStatement = "return" expression ";";
    private Optional<Statement> parseReturnStatement() {
        if (token.type() != TokenType.RETURN_KEYWORD) {
            return Optional.empty();
        }

        consumeToken();

        Expression returnExpression = parseExpression().orElseThrow(() -> new SyntaxError("Missing return expression"));

        expectToken(TokenType.SEMICOLON, "Missing semicolon");

        return Optional.of(new ReturnStatement(returnExpression));
    }

    // whileStatement = "while" "(" expression ")" statementBlock;
    private Optional<Statement> parseWhileStatement() {
        if (token.type() != TokenType.WHILE_KEYWORD) {
            return Optional.empty();
        }

        consumeToken();

        expectToken(TokenType.LEFT_ROUND_BRACKET, "Expected left round bracket after while keyword");

        Expression expression = parseExpression()
                .orElseThrow(() -> new SyntaxError("Missing expression in while statement"));

        expectToken(TokenType.RIGHT_ROUND_BRACKET, "Expected right round bracket after while keyword");

        var block = parseStatementBlock();

        return Optional.of(new WhileStatement(expression, block));
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
            Expression rightLogicFactor = parseAndExpression()
                    .orElseThrow(() -> new SyntaxError("Missing right side of or operator"));

            leftLogicFactor = new OrExpression(leftLogicFactor, rightLogicFactor);
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
            var rightLogicFactor = parseRelationExpression()
                    .orElseThrow(() -> new SyntaxError("Missing right side of && operator"));

            leftLogicFactor = new AndExpression(leftLogicFactor, rightLogicFactor);
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

        TokenType operatorTokenType = token.type();
        if (!operatorTokenType.isRelationalOperator()) {
            return left;
        }

        consumeToken();

        Expression rightLogicFactor = parseAdditiveExpression()
                .orElseThrow(() -> new SyntaxError("Missing right side of < operator"));

        switch (operatorTokenType) {
            case LESS_THAN_OPERATOR -> {
                return Optional.of(new LessThan(leftLogicFactor, rightLogicFactor));
            }
            case LESS_THAN_OR_EQUAL_OPERATOR -> {
                return Optional.of(new LessThanOrEqual(leftLogicFactor, rightLogicFactor));
            }
            case GREATER_THAN_OPERATOR -> {
                return Optional.of(new GreaterThan(leftLogicFactor, rightLogicFactor));
            }
            case GREATER_THAN_OR_EQUAL_OPERATOR -> {
                return Optional.of(new GreaterThanOrEqual(leftLogicFactor, rightLogicFactor));
            }
            case EQUAL_OPERATOR -> {
                return Optional.of(new Equal(leftLogicFactor, rightLogicFactor));
            }
            case NOT_EQUAL_OPERATOR -> {
                return Optional.of(new NotEqual(leftLogicFactor, rightLogicFactor));
            }
            default -> throw new SyntaxError("Unrecognized relational operator");
        }
    }

    private Optional<Expression> parseAdditiveExpression() {
        var left = parseMultiplicativeExpression();
        if (left.isEmpty()) {
            return Optional.empty();
        }

        var leftLogicFactor = left.get();

        while (token.type() == TokenType.PLUS_OPERATOR || token.type() == TokenType.MINUS_OPERATOR) {
            TokenType tokenType = token.type();
            consumeToken();

            Expression rightLogicFactor = parseMultiplicativeExpression()
                    .orElseThrow(() -> new SyntaxError("Missing right side of + operator"));

            if (tokenType == TokenType.PLUS_OPERATOR) {
                leftLogicFactor = new PlusExpression(leftLogicFactor, rightLogicFactor);
            } else {
                leftLogicFactor = new MinusExpression(leftLogicFactor, rightLogicFactor);
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
            var rightLogicFactor = parseNullableExpression()
                    .orElseThrow(() -> new SyntaxError("Missing right side of * operator"));

            if (type == TokenType.MULTIPLICATION_OPERATOR) {
                leftLogicFactor = new MultiplyExpression(leftLogicFactor, rightLogicFactor);
            } else if (type == TokenType.DIVISION_OPERATOR ) {
                leftLogicFactor = new DivideExpression(leftLogicFactor, rightLogicFactor);
            } else {
                leftLogicFactor = new ModuloExpression(leftLogicFactor, rightLogicFactor);
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
            Expression expression = parseCastedExpression()
                    .orElseThrow(() -> new SyntaxError("Expected expression after negation operator"));

            return Optional.of(new NegatedExpression(expression));
        }

        return parseCastedExpression();
    }

    // castedExpression = simpleExpression ["as" simpleType];
    private Optional<Expression> parseCastedExpression() {
        var expression = parseDictKeyExpression();
        if (expression.isEmpty()) {
            return expression;
        }

        if (token.type() == TokenType.AS_KEYWORD) {
            consumeToken();

            if (!isSimpleType(token.type())) {
                throw new SyntaxError("Expected simple type after as keyword");
            }
            Type type = Type.simpleType(token.type());

            consumeToken();
            return Optional.of(new CastedExpression(expression.get(), type));
        }

        return expression;
    }

    // dictKeyExpression = simpleExpression ["[" expression "]"];
    private Optional<Expression> parseDictKeyExpression() {
        var expression = parseSimpleExpression();
        if (expression.isEmpty()) {
            return expression;
        }

        if (token.type() == TokenType.LEFT_SQUARE_BRACKET) {
            consumeToken();

            Optional<Expression> dictKey = parseExpression();
            if (dictKey.isEmpty()) {
                throw new SyntaxError("Missing dict key");
            }

            expectToken(TokenType.RIGHT_SQUARE_BRACKET, "Missing right square bracket in dict key retrieval");

            return Optional.of(new DictValue(expression.get(), dictKey.get()));
        }

        return expression;
    }

    // simpleExpression = identifier | literal | "(" expression ")" | functionCallAsExpression;
    private Optional<Expression> parseSimpleExpression() {
        if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
            consumeToken();

            var expression = parseExpression();
            if (expression.isEmpty()) {
                throw new SyntaxError("Missing expression inside brackets");
            }

            expectToken(TokenType.RIGHT_ROUND_BRACKET, "Missing right round bracket");

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

        if (token.type() == TokenType.NULL_KEYWORD) {
            consumeToken();
            return Optional.of(Null.getInstance());
        }

        if (token.type() == TokenType.LEFT_CURLY_BRACKET) {
            return Optional.of(parseDictLiteral());
        }

        if (token.type() == TokenType.IDENTIFIER) {
            var identifierName = (String) token.value();
            consumeToken();

            if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
                consumeToken();
                List<Expression> arguments = parseArguments();
                expectToken(TokenType.RIGHT_ROUND_BRACKET, "Missing right round bracket in function call");

                return Optional.of(new FunctionCall(identifierName, arguments));
            }

            return Optional.of(new VariableValue(identifierName));
        }

        return Optional.empty();
    }

    // dictLiteral = "{" [expression ":" expression {"," expression ":" expression }] "}";
    private Expression parseDictLiteral() {
        expectToken(TokenType.LEFT_CURLY_BRACKET, "Expected left curly bracket");
        if (token.type() == TokenType.RIGHT_CURLY_BRACKET) {
            return DictLiteral.empty();
        }

        Map<Expression, Expression> content = new HashMap<>();

        var entry = parseDictLiteralKeyValuePair();
        content.put(entry.getKey(), entry.getValue());

        while (token.type() == TokenType.COMMA) {
            consumeToken();

            entry = parseDictLiteralKeyValuePair();
            content.put(entry.getKey(), entry.getValue());
        }

        expectToken(TokenType.RIGHT_CURLY_BRACKET, "Expected right curly bracket");

        return new DictLiteral(content);
    }

    // dictLiteralKeyValuePair = expression ":" expression;
    private AbstractMap.SimpleImmutableEntry<Expression, Expression> parseDictLiteralKeyValuePair() {
        Optional<Expression> key = parseExpression();
        if (key.isEmpty()) {
            throw new SyntaxError("Missing key in dict literal");
        }

        expectToken(TokenType.COLON, "Missing colon after key in dict literal");

        Optional<Expression> value = parseExpression();
        if (value.isEmpty()) {
            throw new SyntaxError("Missing value in dict literal");
        }

        return new AbstractMap.SimpleImmutableEntry<>(key.get(), value.get());
    }

    // arguments = [expression, {"," expression}];
    private List<Expression> parseArguments() {
        List<Expression> arguments = new ArrayList<>();

        var argument = parseExpression();
        if (argument.isEmpty()) {
            return arguments;
        }

        arguments.add(argument.get());

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            argument = parseExpression();
            if (argument.isEmpty()) {
                throw new SyntaxError("Expected argument after comma");
            }
            arguments.add(argument.get());
        }

        return arguments;
    }

    private void consumeToken() {
        this.token = lexer.nextToken();
    }

    private Token expectToken(TokenType expectedType, String errorMessage) {
        if (token.type() != expectedType) {
            throw new SyntaxError(errorMessage);
        }

        var currentToken = token;
        consumeToken();
        return currentToken;
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
