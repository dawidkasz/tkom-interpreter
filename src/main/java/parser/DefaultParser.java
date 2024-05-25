package parser;

import ast.Declaration;
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
import ast.expression.NegationExpression;
import ast.expression.NotEqual;
import ast.expression.Null;
import ast.expression.NullableExpression;
import ast.expression.OrExpression;
import ast.expression.PlusExpression;
import ast.expression.StringLiteral;
import ast.expression.UnaryMinusExpression;
import ast.expression.VariableValue;
import ast.statement.DictAssignment;
import ast.statement.ForeachStatement;
import ast.statement.IfStatement;
import ast.statement.ReturnStatement;
import ast.statement.Statement;
import ast.statement.VariableAssignment;
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;
import ast.type.SimpleType;
import ast.type.Type;
import ast.type.VoidType;
import lexer.Lexer;
import lexer.Position;
import lexer.Token;
import lexer.TokenType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultParser implements Parser {
    private final Lexer lexer;

    private Token token;

    public DefaultParser(Lexer lexer) {
        this.lexer = lexer;
        consumeToken();
    }

    // program = {functionDefinition | variableDeclaration};
    @Override
    public Program parseProgram() {
        Map<String, FunctionDefinition> functions = new HashMap<>();
        Map<String, VariableDeclaration> globalVariables = new HashMap<>();

        var declaration = this.parseFunctionDefinitionOrGlobalVariableDeclaration();
        while (declaration.isPresent()) {

            if (declaration.get() instanceof FunctionDefinition fun) {
                functions.put(fun.name(), fun);
            } else if (declaration.get() instanceof VariableDeclaration globalVar) {
                globalVariables.put(globalVar.name(), globalVar);
            } else {
                throw new IllegalStateException("Unrecognized declaration type");
            }

            declaration = this.parseFunctionDefinitionOrGlobalVariableDeclaration();
        }

        return new Program(functions, globalVariables);
    }

    // functionDefinition = functionReturnType identifier parameterList statementBlock |
    //                      type identifier ["=" expression] ";";;
    private Optional<Declaration> parseFunctionDefinitionOrGlobalVariableDeclaration() {
        if (!token.type().isFunctionReturnType()) {
            return Optional.empty();
        }

        var position = token.position();

        Type type = parseFunctionReturnType().orElseThrow(() -> new SyntaxError("Expected function return type"));

        Token t = expectToken(TokenType.IDENTIFIER, "Expected an identifier");
        var varName = (String) t.value();

        if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
            consumeToken();

            var params = parseParameters();
            expectToken(TokenType.RIGHT_ROUND_BRACKET, "Expected right parentheses");
            var block = parseStatementBlock();
            return Optional.of(new FunctionDefinition(type, varName, params, block, position));
        }

        if (type.equals(new VoidType())) {
            throw new SyntaxError("Variable can't be of type void");
        }

        return Optional.of(parsePartOfVariableDeclarationAfterIdentifier(type, varName, position));
    }

    // parameters = [parameter, {"," parameter}];
    private List<Parameter> parseParameters() {
        List<Parameter> parameters = new ArrayList<>();

        Optional<Parameter> optionalParameter = parseParameter();
        if (optionalParameter.isEmpty()) {
            return parameters;
        }

        Parameter parameter = optionalParameter.get();
        parameters.add(parameter);

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            parameter = parseParameter().orElseThrow(() -> new SyntaxError("Expected a parameter"));

            parameters.add(parameter);
        }

        return parameters;
    }

    // parameter = type identifier;
    private Optional<Parameter> parseParameter() {
        var type = parseType();
        if (type.isEmpty()) {
            return Optional.empty();
        }

        var identifier = expectToken(TokenType.IDENTIFIER, "Expected identifier");

        return Optional.of(new Parameter(type.get(), (String) identifier.value()));
    }

    // functionReturnType = type | "void";
    private Optional<Type> parseFunctionReturnType() {
        TokenType tokenType = token.type();

        if (tokenType == TokenType.VOID_KEYWORD) {
            consumeToken();
            return Optional.of(new VoidType());
        }

        return parseType();
    }

    // type = simpleType | parametrizedType;
    private Optional<Type> parseType() {
        TokenType tokenType = token.type();

        if (tokenType.isSimpleType()) {
            var type = Type.simpleType(token.type());
            consumeToken();
            return Optional.of(type);
        }

        if (tokenType.isCollectionType()) {
            consumeToken();

            expectToken(TokenType.LEFT_SQUARE_BRACKET, "Expected left square bracket");

            if (!token.type().isSimpleType()) {
                throw new SyntaxError("Expected simple type");
            }
            var paramType1 = Type.simpleType(token.type());

            consumeToken();

            expectToken(TokenType.COMMA, "Expected comma");

            if (!token.type().isSimpleType()) {
                throw new SyntaxError("Expected simple type");
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

    // statement = ifStatement | whileStatement | forEachStatement |
    //             variableDeclaration | assignment | functionCall | returnStatement;
    private Optional<Statement> parseStatement() {
        return parseIfStatement()
                .or(this::parseWhileStatement)
                .or(this::parseForeachStatement)
                .or(this::parseVariableDeclaration)
                .or(this::parseAssignmentOrFunctionCall)
                .or(this::parseReturnStatement);
    }

    // ifStatement = "if" "(" expression ")" statementBlock ["else" statementBlock];
    private Optional<Statement> parseIfStatement() {
        if (token.type() != TokenType.IF_KEYWORD) {
            return Optional.empty();
        }

        var position = token.position();

        consumeToken();

        expectToken(TokenType.LEFT_ROUND_BRACKET, "Expected left round bracket");
        Expression condition = parseExpression().orElseThrow(() -> new SyntaxError("Missing condition"));
        expectToken(TokenType.RIGHT_ROUND_BRACKET, "Expected right round bracket");

        List<Statement> ifBlock = parseStatementBlock();

        if (token.type() == TokenType.ELSE_KEYWORD) {
            consumeToken();
            List<Statement> elseBlock = parseStatementBlock();
            return Optional.of(new IfStatement(condition, ifBlock, elseBlock, position));
        }

        return Optional.of(new IfStatement(condition, ifBlock, position));
    }

    // variableDeclaration = type identifier ["=" expression] ";";
    private Optional<Statement> parseVariableDeclaration() {
        var position = token.position();

        Optional<Type> type = parseType();
        if (type.isEmpty()) {
            return Optional.empty();
        }

        var identifier = expectToken(TokenType.IDENTIFIER, "Expected identifier");
        var varName = (String) identifier.value();

        return Optional.of(parsePartOfVariableDeclarationAfterIdentifier(type.get(), varName, position));
    }

    private VariableDeclaration parsePartOfVariableDeclarationAfterIdentifier(Type type, String varName, Position position) {
        if (token.type() == TokenType.ASSIGNMENT) {
            consumeToken();
            var expression = parseExpression().orElseThrow(() -> new SyntaxError("Missing expression"));
            expectToken(TokenType.SEMICOLON, "Expected semicolon");

            return new VariableDeclaration(type, varName, expression, position);
        }

        expectToken(TokenType.SEMICOLON, "Expected semicolon");

        return new VariableDeclaration(type, varName, Null.getInstance(), position);
    }

    // assignmentOrFunctionCall = (identifier "=" expression ";") |
    //         (identifier "[" expression "]" "=" expression ";") |
    //         identifier "(" arguments ")"
    private Optional<Statement> parseAssignmentOrFunctionCall() {
        if (token.type() != TokenType.IDENTIFIER) {
            return Optional.empty();
        }

        String identifierName = (String) token.value();
        var position = token.position();

        consumeToken();

        if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
            consumeToken();
            List<Expression> arguments = parseArguments();
            expectToken(TokenType.RIGHT_ROUND_BRACKET, "Missing right round bracket");
            expectToken(TokenType.SEMICOLON, "Expected semicolon");
            return Optional.of(new FunctionCall(identifierName, arguments, position));
        }

        if (token.type() == TokenType.ASSIGNMENT) {
            consumeToken();
            Expression expression = parseExpression().orElseThrow(() -> new SyntaxError("Expected expression"));
            expectToken(TokenType.SEMICOLON, "Expected semicolon");
            return Optional.of(new VariableAssignment(identifierName, expression, position));
        }

        if (token.type() == TokenType.LEFT_SQUARE_BRACKET) {
            consumeToken();
            Expression key = parseExpression().orElseThrow(() -> new SyntaxError("Expected key"));

            expectToken(TokenType.RIGHT_SQUARE_BRACKET, "Expected right square bracket");
            expectToken(TokenType.ASSIGNMENT, "Expected assignment operator");

            Expression value = parseExpression().orElseThrow(() -> new SyntaxError("Expected value"));

            expectToken(TokenType.SEMICOLON, "Expected semicolon");

            return Optional.of(new DictAssignment(identifierName, key, value, position));
        }

        throw new SyntaxError("Can't parse assignment or function call");
    }

    // whileStatement = "while" "(" expression ")" statementBlock;
    private Optional<Statement> parseWhileStatement() {
        if (token.type() != TokenType.WHILE_KEYWORD) {
            return Optional.empty();
        }

        var position = token.position();

        consumeToken();

        expectToken(TokenType.LEFT_ROUND_BRACKET, "Expected left round bracket after while keyword");

        Expression expression = parseExpression()
                .orElseThrow(() -> new SyntaxError("Expected condition in while statement", position));

        expectToken(TokenType.RIGHT_ROUND_BRACKET, "Expected right round bracket after while keyword");

        var block = parseStatementBlock();

        return Optional.of(new WhileStatement(expression, block, position));
    }

    // foreachStatement = "foreach" "(" simpleType identifier ":" expression ")" statementBlock;
    private Optional<Statement> parseForeachStatement() {
        if (token.type() != TokenType.FOREACH_KEYWORD) {
            return Optional.empty();
        }

        var position = token.position();

        consumeToken();

        expectToken(TokenType.LEFT_ROUND_BRACKET, "Expected left round bracket");

        if (!token.type().isSimpleType()) {
            throw new SyntaxError("Invalid type");
        }

        Type type = parseType().orElseThrow(() -> new SyntaxError("Missing type"));

        var identifier = expectToken(TokenType.IDENTIFIER, "Expected identifier");

        expectToken(TokenType.COLON, "Expected colon");

        Expression iterable = parseExpression()
                .orElseThrow(() -> new SyntaxError("Missing expression in while statement"));

        expectToken(TokenType.RIGHT_ROUND_BRACKET, "Expected right round bracket");

        var block = parseStatementBlock();

        return Optional.of(new ForeachStatement((SimpleType) type, (String) identifier.value(), iterable, block, position));
    }

    // returnStatement = "return" expression ";";
    private Optional<Statement> parseReturnStatement() {
        if (token.type() != TokenType.RETURN_KEYWORD) {
            return Optional.empty();
        }

        var position = token.position();

        consumeToken();

        Expression returnExpression = parseExpression().orElseThrow(() -> new SyntaxError("Missing return expression"));

        expectToken(TokenType.SEMICOLON, "Missing semicolon");

        return Optional.of(new ReturnStatement(returnExpression, position));
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
                    .orElseThrow(() -> new SyntaxError("Missing right side of || operator"));

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
        if (token.type() == TokenType.NEGATION_OPERATOR) {
            consumeToken();
            Expression expression = parseCastedExpression()
                    .orElseThrow(() -> new SyntaxError("Expected expression after negation operator"));

            return Optional.of(new NegationExpression(expression));
        }

        if (token.type() == TokenType.MINUS_OPERATOR) {
            consumeToken();
            Expression expression = parseCastedExpression()
                    .orElseThrow(() -> new SyntaxError("Expected expression after unary minus operator"));

            return Optional.of(new UnaryMinusExpression(expression));
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

            if (!token.type().isSimpleType()) {
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

            Expression dictKey = parseExpression().orElseThrow(() -> new SyntaxError("Missing dict key"));

            expectToken(TokenType.RIGHT_SQUARE_BRACKET, "Missing right square bracket in dict key retrieval");

            return Optional.of(new DictValue(expression.get(), dictKey));
        }

        return expression;
    }

    // simpleExpression = identifier | literal | "(" expression ")" | functionCallAsExpression;
    private Optional<Expression> parseSimpleExpression() {
        var position = token.position();

        if (token.type() == TokenType.LEFT_ROUND_BRACKET) {
            consumeToken();

            Expression expression = parseExpression().orElseThrow(() -> new SyntaxError("Missing expression inside brackets"));

            expectToken(TokenType.RIGHT_ROUND_BRACKET, "Missing right round bracket");

            return Optional.of(expression);
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

                return Optional.of(new FunctionCall(identifierName, arguments, position));
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
        Expression key = parseExpression().orElseThrow(() -> new SyntaxError("Missing key in dict literal"));

        expectToken(TokenType.COLON, "Missing colon after key in dict literal");

        Expression value = parseExpression().orElseThrow(() -> new SyntaxError("Missing value in dict literal"));

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    // arguments = [expression, {"," expression}];
    private List<Expression> parseArguments() {
        List<Expression> arguments = new ArrayList<>();

        Optional<Expression> optionalArgument = parseExpression();
        if (optionalArgument.isEmpty()) {
            return arguments;
        }

        Expression argument = optionalArgument.get();
        arguments.add(argument);

        while (token.type() == TokenType.COMMA) {
            consumeToken();
            argument = parseExpression().orElseThrow(() -> new SyntaxError("Expected argument after comma"));
            arguments.add(argument);
        }

        return arguments;
    }

    private void consumeToken() {
        this.token = lexer.nextToken();
    }

    private Token expectToken(TokenType expectedType, String errorMessage) {
        if (token.type() != expectedType) {
            throw new SyntaxError(errorMessage, token.position());
        }

        var currentToken = token;
        consumeToken();
        return currentToken;
    }

    public static class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }

        SyntaxError(String message, Position position) {
            super(message + String.format(" at position(line=%s, column=%s)", position.lineNumber(), position.columnNumber()));
        }
    }
}
