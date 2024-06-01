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
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.Type;
import ast.type.VoidType;
import lexer.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public class SemanticChecker implements AstVisitor {
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    private final ResultStore<Type> lastType = new ResultStore<>();
    private FunctionCallContext currentFunctionContext;
    private Scope globalScope;

    @Override
    public void visit(Program program) {
        functions.clear();
        globalScope = new Scope();
        currentFunctionContext = null;

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
            throw new SemanticException(String.format("Missing return statement in function %s at %s",
                    functionDefinition.name(), functionDefinition.position()));
        }

        if (returnStatements.size() > 1) {
            throw new SemanticException(String.format("Unreachable return statement in function %s at %s",
                    functionDefinition.name(), returnStatements.get(1).position()));
        }

        functionDefinition.statementBlock().accept(this);
        currentFunctionContext = null;
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        String varName = variableAssignment.varName();

        Type varType = currentFunctionContext.findVar(varName)
                .orElseThrow(() -> new SemanticException(String.format("Variable %s is not defined", varName)))
                .getType();

        variableAssignment.expression().accept(this);

        assertTypesMatch(varType, lastType.fetchAndReset(), variableAssignment.position());
    }

    @Override
    public void visit(DictAssignment dictAssignment) {

    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.condition().accept(this);
        whileStatement.statementBlock().accept(this);
    }

    @Override
    public void visit(ForeachStatement foreachStatement) {
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
                    "Expected function to return %s at %s, but received %s instead",
                    functionReturnType,
                    returnStatement.position(),
                    returnType
            ));
        }
    }

    @Override
    public void visit(FunctionCall functionCall) {
        if (functionCall.functionName().equals("print")) {
            return;
        }

        FunctionDefinition funDef = Optional.ofNullable(functions.get(functionCall.functionName()))
                .orElseThrow(() -> new SemanticException(String.format("Function %s is not defined", functionCall.functionName())));

        int numParams = funDef.parameters().size();
        int numArgs = functionCall.arguments().size();

        if (numArgs != numParams) {
            throw new SemanticException(String.format(
                    "Expected %s arguments, but provided %s at %s", numParams, numArgs, functionCall.position()));
        }

        for (int argIdx=0; argIdx<numArgs; ++argIdx) {
            functionCall.arguments().get(argIdx).accept(this);
            Type argType = lastType.fetchAndReset();
            Type paramType = funDef.parameters().get(argIdx).type();

            if (!argType.equals(paramType)) {
                throw new TypesMismatchException(argType, paramType);
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

    }

    @Override
    public void visit(DictLiteral dictLiteral) {
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
    }

    public static class TypesMismatchException extends SemanticException {
        public TypesMismatchException(Type t1, Type t2, Position position) {
            super(String.format("Types %s and %s do not match at %s", t1, t2, position));
        }

        public TypesMismatchException(Type t1, Type t2) {
            super(String.format("Types %s and %s do not match", t1, t2));
        }
    }

    private record NullAnyType() implements Type {
    }
}
