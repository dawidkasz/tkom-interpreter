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
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;

public class DefaultProgramExecutor implements AstVisitor, ProgramExecutor {
    private final Stack<FunctionCallContext> callStack = new Stack<>();
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    private final Map<String, Runnable> builtinFunctions;
    private LastResult lastResult;
    private Scope globalScope;
    private boolean shouldReturnFromCurrentFunctionCall = false;

    public DefaultProgramExecutor() {
        builtinFunctions = Map.of(
                "print", this::executeBuiltinPrint
        );
    }

    private void executeBuiltinPrint() {
        var context = callStack.peek();
        Variable arg0 = context.findVar("arg0").orElseThrow();

        if (!arg0.getType().equals(String.class)) {
            throw new RuntimeException("Invalid print arg type " + arg0.getClass());
        }

        System.out.println((String) arg0.getValue());

        callStack.pop();
    }

    @Override
    public void execute(Program program) {
        resetState();
        visit(program);
    }

    private void resetState() {
        callStack.clear();
        functions.clear();
        globalScope = new Scope();
        lastResult = new LastResult();
    }

    @Override
    public void visit(Program program) {
        functions.putAll(program.functions());

        program.globalVariables().forEach((varName, varDec) -> {
            varDec.value().accept(this);
            globalScope.declareVar(new Variable(
                    varName,
                    Variable.getInterpreterType(varDec.type()),
                    lastResult.fetchAndReset()
            ));
        });

        callStack.push(new FunctionCallContext("main", new Scope(), globalScope));
        program.functions().get("main").statementBlock().accept(this);
    }

    @Override
    public void visit(FunctionDefinition functionDefinition) {
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        FunctionCallContext context = callStack.peek();

        var varType = context.findVar(variableAssignment.varName())
                .or(() -> globalScope.get(variableAssignment.varName()))
                .orElseThrow()
                .getType();

        variableAssignment.expression().accept(this);
        Object newValue = lastResult.fetchAndReset();

        if (newValue.equals(Null.getInstance()) || varType.equals(newValue.getClass())) {
            context.assignVar(variableAssignment.varName(), newValue);
        } else {
            throw new RuntimeException("Invalid variable assignment");
        }
    }

    @Override
    public void visit(DictAssignment dictAssignment) {

    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.condition().accept(this);
        boolean shouldLoop = isTruthy(lastResult.fetchAndReset());

        while (shouldLoop) {
            whileStatement.statementBlock().accept(this);
            whileStatement.condition().accept(this);
            shouldLoop = isTruthy(lastResult.fetchAndReset());
        }
    }

    @Override
    public void visit(ForeachStatement foreachStatement) {

    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.condition().accept(this);
        var conditionValue = lastResult.fetchAndReset();

        if (isTruthy(conditionValue)) {
            ifStatement.ifBlock().accept(this);
        } else if (ifStatement.elseBlock() != null) {
            ifStatement.elseBlock().accept(this);
        }
    }

    @Override
    public void visit(ReturnStatement returnStatement) {
        returnStatement.expression().accept(this);
        shouldReturnFromCurrentFunctionCall = true;
    }

    @Override
    public void visit(FunctionCall functionCall) {
        FunctionDefinition functionDef = functions.get(functionCall.functionName());
        if (functionDef != null) {
            if (functionCall.arguments().size() != functionDef.parameters().size()) {
                throw new RuntimeException("Invalid number of arguments in function call");
            }

            List<Variable> arguments = new ArrayList<>();

            for(int idx = 0; idx < functionCall.arguments().size(); idx++) {
                functionCall.arguments().get(idx).accept(this);
                var argValue = lastResult.fetchAndReset();

                String paramName = functionDef.parameters().get(idx).name();
                Type paramType = functionDef.parameters().get(idx).type();

                if (
                        !Variable.getInterpreterType(paramType).equals(argValue.getClass()) &&
                        !argValue.equals(Null.getInstance())
                ) {
                    throw new RuntimeException(String.format("Types mismatch between param %s and argument", paramName));
                }

                arguments.add(new Variable(
                        paramName,
                        Variable.getInterpreterType(functionDef.parameters().get(idx).type()),
                        argValue
                ));
            }

            callStack.push(new FunctionCallContext(
                    functionCall.functionName(),
                    new Scope(arguments),
                    globalScope
            ));

            functionDef.statementBlock().accept(this);

            callStack.pop();
            shouldReturnFromCurrentFunctionCall = false;

            return;
        }

        Runnable builtinFunctionDef = builtinFunctions.get(functionCall.functionName());
        if (builtinFunctionDef != null) {
            List<Variable> arguments = new ArrayList<>();

            for(int idx = 0; idx < functionCall.arguments().size(); idx++) {
                functionCall.arguments().get(idx).accept(this);

                arguments.add(new Variable("arg" + idx, String.class, lastResult.fetchAndReset()));
            }

            callStack.push(new FunctionCallContext(
                    functionCall.functionName(),
                    new Scope(arguments),
                    globalScope
            ));
            builtinFunctionDef.run();
            return;
        }

        throw new RuntimeException("Unrecognized function name " + functionCall.functionName());
    }

    @Override
    public void visit(DivideExpression divideExpression) {
        divideExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        divideExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left / (Integer) right);
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store((Float) left / (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(CastedExpression castedExpression) {
        castedExpression.expression().accept(this);
        var value = lastResult.fetchAndReset();

        Class<?> clazz = value.getClass();
        var outType = castedExpression.asType();

        if (clazz.equals(Integer.class)) {
            if (outType.equals(new IntType())) {
                lastResult.store(value);
            } else if (outType.equals(new FloatType())) {
                lastResult.store(Float.valueOf((Integer) value));
            } else if (outType.equals(new StringType())) {
                lastResult.store(String.valueOf(value));
            }
        } else if (clazz.equals(Float.class)) {
            if (outType.equals(new IntType())) {
                lastResult.store(((Float) value).intValue());
            } else if (outType.equals(new FloatType())) {
                lastResult.store(value);
            } else if (outType.equals(new StringType())) {
                lastResult.store(String.valueOf(value));
            }
         } else if (clazz.equals(String.class)) {
            if (outType.equals(new IntType())) {
                lastResult.store(Integer.valueOf((String) value));
            } else if (outType.equals(new FloatType())) {
                lastResult.store(Float.valueOf((String) value));
            } else if (outType.equals(new StringType())) {
                lastResult.store(value);
            }
        } else if (clazz.equals(Null.class)) {
            if (outType.equals(new StringType())) {
                lastResult.store(value.toString());
            }
        }
    }

    @Override
    public void visit(DictValue dictValue) {

    }

    @Override
    public void visit(DictLiteral dictLiteral) {

    }

    @Override
    public void visit(FloatLiteral floatLiteral) {
        lastResult.store(floatLiteral.value());
    }

    @Override
    public void visit(IntLiteral intLiteral) {
        lastResult.store(intLiteral.value());
    }

    @Override
    public void visit(StringLiteral stringLiteral) {
        lastResult.store(stringLiteral.value());
    }

    @Override
    public void visit(MinusExpression minusExpression) {
        minusExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        minusExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left - (Integer) right);
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store((Float) left - (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(ModuloExpression moduloExpression) {
        moduloExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        moduloExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left % (Integer) right);
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store((Float) left % (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(MultiplyExpression multiplyExpression) {
        multiplyExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        multiplyExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left * (Integer) right);
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store((Float) left * (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(Null aNull) {
        lastResult.store(Null.getInstance());
    }

    @Override
    public void visit(VariableValue variableValue) {
        Object varValue = callStack.peek().findVar(variableValue.varName())
                .or(() -> globalScope.get(variableValue.varName()))
                .orElseThrow()
                .getValue();

        lastResult.store(varValue);
    }

    @Override
    public void visit(PlusExpression plusExpression) {
        plusExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        plusExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left + (Integer) right);
        } else if (validateMatchingTypes(left, right, String.class)) {
            lastResult.store((String) left + (String) right);
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store((Float) left + (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    private boolean validateMatchingTypes(Object o1, Object o2, Class<?> clazz) {
        return o1.getClass().equals(clazz) && o1.getClass().equals(o2.getClass());
    }

    @Override
    public void visit(NullableExpression nullableExpression) {
        Object value;

        try {
            nullableExpression.expression().accept(this);
            value = lastResult.fetchAndReset();
        } catch (NullException e) {
            value = Null.getInstance();
        }

        lastResult.store(value);
    }

    @Override
    public void visit(LessThan lessThan) {
        lessThan.left().accept(this);
        var left = lastResult.fetchAndReset();

        lessThan.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store(booleanToInteger((Integer) left < (Integer) right));
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store(booleanToInteger((Float) left < (Float) right));
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        greaterThan.left().accept(this);
        var left = lastResult.fetchAndReset();

        greaterThan.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store(booleanToInteger((Integer) left > (Integer) right));
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store(booleanToInteger((Float) left > (Float) right));
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(LessThanOrEqual lessThanOrEqual) {
        lessThanOrEqual.left().accept(this);
        var left = lastResult.fetchAndReset();

        lessThanOrEqual.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store(booleanToInteger((Integer) left <= (Integer) right));
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store(booleanToInteger((Float) left <= (Float) right));
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(GreaterThanOrEqual greaterThanOrEqual) {
        greaterThanOrEqual.left().accept(this);
        var left = lastResult.fetchAndReset();

        greaterThanOrEqual.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateMatchingTypes(left, right, Integer.class)) {
            lastResult.store(booleanToInteger((Integer) left >= (Integer) right));
        } else if (validateMatchingTypes(left, right, Float.class)) {
            lastResult.store(booleanToInteger((Float) left >= (Float) right));
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        if (!isTruthy(left)) {
            lastResult.store(0);
            return;
        }

        andExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        lastResult.store(booleanToInteger(isTruthy(right)));
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        if (isTruthy(left)) {
            lastResult.store(1);
            return;
        }

        orExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        lastResult.store(booleanToInteger(isTruthy(right)));
    }

    @Override
    public void visit(Equal equal) {
        equal.left().accept(this);
        var left = lastResult.fetchAndReset();

        equal.right().accept(this);
        var right = lastResult.fetchAndReset();

        if (!left.getClass().equals(right.getClass())) {
            throw new RuntimeException("Types do not match");
        }

        lastResult.store(booleanToInteger(left.equals(right)));
    }

    @Override
    public void visit(NotEqual notEqual) {
        notEqual.left().accept(this);
        var left = lastResult.fetchAndReset();

        notEqual.right().accept(this);
        var right = lastResult.fetchAndReset();

        if (!left.getClass().equals(right.getClass())) {
            throw new RuntimeException("Types do not match");
        }

        lastResult.store(booleanToInteger(!left.equals(right)));
    }

    @Override
    public void visit(NegationExpression negationExpression) {
        negationExpression.expression().accept(this);
        var value = lastResult.fetchAndReset();

        lastResult.store(booleanToInteger(!isTruthy(value)));
    }

    @Override
    public void visit(UnaryMinusExpression unaryMinusExpression) {
        unaryMinusExpression.expression().accept(this);
        var value = lastResult.fetchAndReset();

        if (value.getClass().equals(Integer.class)) {
            lastResult.store(-((Integer) value));
        } else if (value.getClass().equals(Float.class)) {
            lastResult.store(-((Float) value));
        } else {
            throw new RuntimeException("invalid type for unary minus");
        }
    }

    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        FunctionCallContext context = callStack.peek();

        variableDeclaration.value().accept(this);
        Object value = lastResult.fetchAndReset();

        if (
                !value.equals(Null.getInstance()) && (value.getClass().equals(Integer.class) && !variableDeclaration.type().equals(new IntType()) ||
                value.getClass().equals(String.class) && !variableDeclaration.type().equals(new StringType()) ||
                value.getClass().equals(Float.class) && !variableDeclaration.type().equals(new FloatType()))
        ) {
            throw new RuntimeException("Types do not match");
        }

        context.declareVar(new Variable(
                variableDeclaration.name(),
                Variable.getInterpreterType(variableDeclaration.type()),
                value
        ));
    }

    @Override
    public void visit(StatementBlock statementBlock) {
        callStack.peek().addScope();

        for (int i=0; i<statementBlock.statements().size(); i++) {
            Statement statement = statementBlock.statements().get(i);
            statement.accept(this);

            if (shouldReturnFromCurrentFunctionCall) {
                return;
            }
        }

        callStack.peek().removeScope();
    }

    private void assertNotNull(Object value) {
        if (value.equals(Null.getInstance())) {
            throw new NullException();
        }
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Integer) {
            return (Integer) value != 0;
        }

        if (value instanceof Float) {
            return (Float) value != 0.0;
        }

        if (value instanceof String) {
            return !(((String) value).isEmpty());
        }

        if (value instanceof Null) {
            return false;
        }

        throw new RuntimeException("Unrecognized value type");
    }

    private int booleanToInteger(boolean value) {
        return value ? 1 : 0;
    }

    public static class NullException extends RuntimeException {
    }
}
