package executor;

import ast.AstVisitor;
import ast.FunctionCall;
import ast.FunctionDefinition;
import ast.Program;
import ast.StatementBlock;
import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DictLiteral;
import ast.expression.DictValue;
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
import ast.statement.VariableAssignment;
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;
import ast.type.DictType;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.SimpleType;
import ast.type.StringType;
import ast.type.Type;
import ast.type.VoidType;
import lexer.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultSemanticChecker implements AstVisitor, SemanticChecker {
    private FunctionCallContext currentFunctionContext;
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    private final ResultStore<Type> lastType = new ResultStore<>();
    private Scope globalScope;

    @Override
    public void verify(Program program) {
        resetState();
        visit(program);
    }

    private void resetState() {
        functions.clear();
        globalScope = new Scope();
        currentFunctionContext = null;
    }

    @Override
    public void visit(Program program) {
        program.globalVariables().forEach((varName, varDec) -> {
            varDec.accept(this);
            globalScope.declareVar(new Variable(varName, varDec.type()));
        });

        functions.putAll(program.functions());

        FunctionDefinition mainFunction = functions.get("main");

        if (mainFunction == null) {
            throw new SemanticException("Main function is not defined");
        }

        if (!mainFunction.returnType().equals(new VoidType())) {
            throw new SemanticException("Main function should return void");
        }

        if (!mainFunction.parameters().isEmpty()) {
            throw new SemanticException("Main function should not have any parameters");
        }

        program.functions().forEach((funName, funDef) -> funDef.accept(this));
    }

    @Override
    public void visit(FunctionDefinition functionDefinition) {
        var argsScope = new Scope();
        functionDefinition.parameters().forEach(param ->
                argsScope.declareVar(new Variable(param.name(), param.type()))
        );

        currentFunctionContext = new FunctionCallContext(
                functionDefinition.name(),
                argsScope,
                globalScope
        );

        List<ReturnStatement> returnStatements = functionDefinition.statementBlock().statements().stream()
                .filter(s -> s instanceof ReturnStatement)
                .map(s -> (ReturnStatement) s)
                .toList();

        if (returnStatements.isEmpty() && !functionDefinition.returnType().equals(new VoidType())) {
            throw new SemanticException(String.format(
                    "Missing return statement in function %s at line %s, column %s",
                    functionDefinition.name(), functionDefinition.position().lineNumber(),
                    functionDefinition.position().columnNumber()));
        }

        if (returnStatements.size() > 1) {
            throw new SemanticException(String.format("Unreachable return statement in function %s at line %s, column %s",
                    functionDefinition.name(), returnStatements.get(1).position().lineNumber(),
                    returnStatements.get(1).position().columnNumber()));
        }

        functionDefinition.statementBlock().accept(this);
        currentFunctionContext = null;
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        String varName = variableAssignment.varName();

        Type varType = currentFunctionContext.findVar(varName)
                .orElseThrow(() -> SemanticException.undefinedVariable(varName))
                .getType();

        variableAssignment.expression().accept(this);

        assertTypesMatch(varType, lastType.fetchAndReset(), variableAssignment.position());
    }

    @Override
    public void visit(DictAssignment dictAssignment) {
        Variable dict = currentFunctionContext.findVar(dictAssignment.variableName())
                .orElseThrow(() -> SemanticException.undefinedVariable(dictAssignment.variableName()));

        if (!(dict.getType() instanceof DictType)) {
            throw new SemanticException(String.format("Expected %s to be of type dict at %s",
                    dictAssignment.variableName(), dictAssignment.position()));
        }

        SimpleType dictKeyType = ((DictType) dict.getType()).keyType();
        SimpleType dictValueType = ((DictType) dict.getType()).valueType();

        dictAssignment.key().accept(this);
        Type keyType = lastType.fetchAndReset();

        if (!keyType.equals(new NullAnyType()) && !keyType.equals(dictKeyType)) {
            throw new TypesMismatchException(keyType, dictKeyType, dictAssignment.position());
        }

        dictAssignment.value().accept(this);
        Type valueType = lastType.fetchAndReset();

        if (!valueType.equals(new NullAnyType()) && !valueType.equals(dictValueType)) {
            throw new TypesMismatchException(valueType, dictValueType, dictAssignment.position());
        }
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.condition().accept(this);
        whileStatement.statementBlock().accept(this);
    }

    @Override
    public void visit(ForeachStatement foreachStatement) {
        foreachStatement.iterable().accept(this);
        Type iterableType = lastType.fetchAndReset();

        if (!(iterableType instanceof DictType iterableTypeDict)) {
            throw new SemanticException(String.format("Value is not iterable at line %s, column %s",
                    foreachStatement.position().lineNumber(), foreachStatement.position().columnNumber()));
        }

        if (!foreachStatement.varType().equals(iterableTypeDict.keyType())) {
            throw new TypesMismatchException(iterableTypeDict.keyType(),
                    foreachStatement.varType(), foreachStatement.position());
        }

        currentFunctionContext.addScope();
        currentFunctionContext.declareVar(new Variable(foreachStatement.varName(), foreachStatement.varType()));
        foreachStatement.statementBlock().accept(this);
        currentFunctionContext.removeScope();
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.condition().accept(this);
        ifStatement.ifBlock().accept(this);

        if (ifStatement.elseBlock() != null) {
            ifStatement.elseBlock().accept(this);
        }
    }

    @Override
    public void visit(ReturnStatement returnStatement) {
        returnStatement.expression().accept(this);
        Type returnType = lastType.fetchAndReset();

        Type functionReturnType = functions.get(currentFunctionContext.getFunctionName()).returnType();

        if (
                !returnType.equals(functionReturnType) &&
                !returnType.equals(new NullAnyType())
        ) {
            throw new SemanticException(String.format(
                    "Expected function %s to return %s at line %s, column %s, but received %s instead",
                    currentFunctionContext.getFunctionName(),
                    functionReturnType,
                    returnStatement.position().lineNumber(),
                    returnStatement.position().columnNumber(),
                    returnType
            ));
        }
    }

    @Override
    public void visit(FunctionCall functionCall) {
        if (functionCall.functionName().equals("print")) {
            return;
        }
        if (functionCall.functionName().equals("input")) {
            lastType.store(new StringType());
            return;
        }

        FunctionDefinition funDef = Optional.ofNullable(functions.get(functionCall.functionName()))
                .orElseThrow(() -> new SemanticException(String.format(
                        "Function %s is not defined at line %s, column %s",
                        functionCall.functionName(),
                        functionCall.position().lineNumber(),
                        functionCall.position().columnNumber()
                )));

        int numParams = funDef.parameters().size();
        int numArgs = functionCall.arguments().size();

        if (numArgs != numParams) {
            throw new SemanticException(String.format("Function %s expected %s arguments, but provided %s at line %s, column %s",
                    functionCall.functionName(), numParams, numArgs,
                    functionCall.position().lineNumber(), functionCall.position().columnNumber()));
        }

        for (int argIdx=0; argIdx<numArgs; ++argIdx) {
            functionCall.arguments().get(argIdx).accept(this);
            Type argType = lastType.fetchAndReset();
            Type paramType = funDef.parameters().get(argIdx).type();

            if (!argType.equals(paramType) && !argType.equals(new NullAnyType())) {
                throw new TypesMismatchException(argType, paramType, functionCall.position());
            }
        }

        lastType.store(funDef.returnType());
    }

    @Override
    public void visit(DivideExpression divideExpression) {
        visitBinaryExpression(divideExpression.left(), divideExpression.right());
    }

    @Override
    public void visit(CastedExpression castedExpression) {
        castedExpression.expression().accept(this);
        lastType.store(castedExpression.asType());
    }

    @Override
    public void visit(DictValue dictValue) {
        dictValue.dict().accept(this);
        Type dict = lastType.fetchAndReset();

        if (!(dict instanceof DictType)) {
            throw new SemanticException(String.format("%s is not a valid dict", dictValue));
        }

        Type dictKeyType = ((DictType) dict).keyType();
        Type dictValueType = ((DictType) dict).valueType();

        dictValue.key().accept(this);
        Type keyType = lastType.fetchAndReset();

        if (!keyType.equals(new NullAnyType()) && !dictKeyType.equals(keyType)) {
            throw new SemanticException(String.format("Expected type %s, but provided %s", dictKeyType, keyType));
        }

        lastType.store(dictValueType);
    }

    @Override
    public void visit(DictLiteral dictLiteral) {
        Set<Type> keyTypes = dictLiteral.content().keySet().stream()
                .map(k -> {
                    k.accept(this);
                    return lastType.fetchAndReset();
                })
                .filter(k -> !k.equals(new NullAnyType()))
                .collect(Collectors.toSet());

        Set<Type> valueTypes = dictLiteral.content().values().stream()
                .map(v -> {
                    v.accept(this);
                    return lastType.fetchAndReset();
                })
                .filter(v -> !v.equals(new NullAnyType()))
                .collect(Collectors.toSet());

        if (keyTypes.size() > 1 || valueTypes.size() > 1) {
            throw new SemanticException("Inconsistent types in dict literal");
        }


        if (keyTypes.isEmpty() && valueTypes.isEmpty()) {
            lastType.store(new DictType(new NullAnyType(), new NullAnyType()));
            return;
        }

        Type keyType = keyTypes.isEmpty() ? new NullAnyType() : keyTypes.stream().findAny().orElseThrow();
        Type valueType = valueTypes.isEmpty() ? new NullAnyType() : valueTypes.stream().findAny().orElseThrow();

        if (!(keyType instanceof SimpleType && valueType instanceof SimpleType)) {
            throw new SemanticException("Only simple types are allowed in dict");
        }

        lastType.store(new DictType((SimpleType) keyType, (SimpleType) valueType));
    }

    @Override
    public void visit(AndExpression andExpression) {
        visitBinaryExpression(andExpression.left(), andExpression.right());
    }

    @Override
    public void visit(FloatLiteral floatLiteral) {
        lastType.store(new FloatType());
    }

    @Override
    public void visit(IntLiteral intLiteral) {
        lastType.store(new IntType());
    }

    @Override
    public void visit(StringLiteral stringLiteral) {
        lastType.store(new StringType());
    }

    @Override
    public void visit(LessThan lessThan) {
        lessThan.left().accept(this);
        Type left = lastType.fetchAndReset();

        lessThan.right().accept(this);
        Type right = lastType.fetchAndReset();

        assertTypesMatch(left, right);
    }

    @Override
    public void visit(MinusExpression minusExpression) {
        minusExpression.left().accept(this);
        Type left = lastType.fetchAndReset();

        minusExpression.right().accept(this);
        Type right = lastType.fetchAndReset();

        assertTypesMatch(left, right);
    }

    @Override
    public void visit(ModuloExpression moduloExpression) {
        visitBinaryExpression(moduloExpression.left(), moduloExpression.right());
    }

    @Override
    public void visit(MultiplyExpression multiplyExpression) {
        visitBinaryExpression(multiplyExpression.left(), multiplyExpression.right());
    }

    @Override
    public void visit(NegationExpression negationExpression) {
        negationExpression.expression().accept(this);
    }

    @Override
    public void visit(Null aNull) {
        lastType.store(new NullAnyType());
    }

    @Override
    public void visit(OrExpression orExpression) {
        visitBinaryExpression(orExpression.left(), orExpression.right());
    }

    @Override
    public void visit(VariableValue variableValue) {
        String varName = variableValue.varName();

        Type varType = currentFunctionContext.findVar(varName)
                .or(() -> globalScope.get(varName))
                .orElseThrow(() -> SemanticException.undefinedVariable(varName))
                .getType();

        lastType.store(varType);
    }

    @Override
    public void visit(PlusExpression plusExpression) {
        visitBinaryExpression(plusExpression.left(), plusExpression.right());
    }

    @Override
    public void visit(NullableExpression nullableExpression) {
        nullableExpression.expression().accept(this);
    }

    @Override
    public void visit(LessThanOrEqual lessThanOrEqual) {
        visitBinaryExpression(lessThanOrEqual.left(), lessThanOrEqual.right());
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        visitBinaryExpression(greaterThan.left(), greaterThan.right());
    }

    @Override
    public void visit(GreaterThanOrEqual greaterThanOrEqual) {
        visitBinaryExpression(greaterThanOrEqual.left(), greaterThanOrEqual.right());
    }

    @Override
    public void visit(Equal equal) {
        visitBinaryExpression(equal.left(), equal.right());
    }

    @Override
    public void visit(NotEqual notEqual) {
        visitBinaryExpression(notEqual.left(), notEqual.right());
    }

    @Override
    public void visit(UnaryMinusExpression unaryMinusExpression) {
        unaryMinusExpression.expression().accept(this);
        Type type = lastType.fetchAndReset();

        if (type.equals(new VoidType()) || type.equals(new StringType())) {
            throw new SemanticException("Unary minus operator is not applicable to type " + type);
        }

        lastType.store(type);
    }

    private void visitBinaryExpression(Expression leftExpression, Expression rightExpression) {
        leftExpression.accept(this);
        Type left = lastType.fetchAndReset();

        rightExpression.accept(this);
        Type right = lastType.fetchAndReset();

        assertTypesMatch(left, right);

        lastType.store(left);
    }

    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        variableDeclaration.value().accept(this);
        Type valueType = lastType.fetchAndReset();

        if (
                variableDeclaration.type() instanceof DictType varType &&
                valueType instanceof DictType valType
        ) {
            SimpleType kType = valType.keyType().equals(new NullAnyType()) ?
                    varType.keyType() : valType.keyType();

            SimpleType vType = valType.valueType().equals(new NullAnyType()) ?
                    varType.valueType() : valType.valueType();

            valueType = new DictType(kType, vType);
        }

        assertTypesMatch(variableDeclaration.type(), valueType, variableDeclaration.position());

        if (currentFunctionContext != null) {
            try {
                currentFunctionContext.declareVar(new Variable(
                        variableDeclaration.name(),
                        variableDeclaration.type()
                ));
            } catch (IllegalArgumentException e) {
                throw new SemanticException(String.format("Variable %s is already defined", variableDeclaration.name()));
            }
        }
    }

    @Override
    public void visit(StatementBlock statementBlock) {
        currentFunctionContext.addScope();
        statementBlock.statements().forEach(statement -> statement.accept(this));
        currentFunctionContext.removeScope();
    }

    private void assertTypesMatch(Type t1, Type t2, Position position) {
        if (!typesMatch(t1, t2)) {
            throw new TypesMismatchException(t1, t2, position);
        }
    }

    private void assertTypesMatch(Type t1, Type t2) {
        if (!typesMatch(t1, t2)) {
            throw new TypesMismatchException(t1, t2);
        }
    }

    private boolean typesMatch(Type t1, Type t2) {
        return t1.equals(t2) || t1.equals(new NullAnyType()) || t2.equals(new NullAnyType());
    }


    public static class SemanticException extends RuntimeException {
        public SemanticException(String message) {
            super(message);
        }

        public static SemanticException undefinedVariable(String varName) {
            return new SemanticException(String.format("Variable %s is not defined", varName));
        }
    }

    public static class TypesMismatchException extends SemanticException {
        public TypesMismatchException(Type t1, Type t2, Position position) {
            super(String.format("Types %s and %s do not match at line %s, column %s",
                    t1, t2, position.lineNumber(), position.columnNumber()));
        }

        public TypesMismatchException(Type t1, Type t2) {
            super(String.format("Types %s and %s do not match", t1, t2));
        }
    }

    private record NullAnyType() implements SimpleType {
    }
}
