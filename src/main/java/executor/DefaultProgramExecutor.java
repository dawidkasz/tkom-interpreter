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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.List;

public class DefaultProgramExecutor implements AstVisitor, ProgramExecutor {
    private final SemanticChecker semanticChecker;
    private final Stack<FunctionCallContext> callStack = new Stack<>();
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    private final Map<String, Runnable> builtinFunctions;
    private ResultStore<Object> lastResult;
    private Scope globalScope;
    private boolean shouldReturnFromCurrentFunctionCall = false;

    public DefaultProgramExecutor(SemanticChecker semanticChecker) {
        this.semanticChecker = semanticChecker;
        builtinFunctions = Map.of(
                "print", this::executeBuiltinPrint,
                "input", this::executeBuiltinInput
        );
    }

    @Override
    public void execute(Program program) {
        semanticChecker.verify(program);
        resetState();
        visit(program);
    }

    private void resetState() {
        callStack.clear();
        functions.clear();
        globalScope = new Scope();
        lastResult = new ResultStore<>();
    }

    @Override
    public void visit(Program program) {
        functions.putAll(program.functions());

        program.globalVariables().forEach((varName, varDec) -> {
            varDec.value().accept(this);
            globalScope.declareVar(new Variable(
                    varName,
                    varDec.type(),
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

        variableAssignment.expression().accept(this);
        context.assignVar(variableAssignment.varName(),  lastResult.fetchAndReset());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(DictAssignment dictAssignment) {
        Variable dictVar = callStack.peek().findVar(dictAssignment.variableName()).orElseThrow();

        dictAssignment.key().accept(this);
        Object key = lastResult.fetchAndReset();

        dictAssignment.value().accept(this);
        Object value = lastResult.fetchAndReset();

        if (dictVar.getValue() instanceof Map) {
            Map<Object, Object> previousVal = (LinkedHashMap<Object, Object>) dictVar.getValue();
            previousVal.put(key, value);
        } else if (dictVar.getValue() instanceof Null) {
            Map<Object, Object> newValue = new LinkedHashMap<>();
            newValue.put(key, value);
            dictVar.setValue(newValue);
        }
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
        foreachStatement.iterable().accept(this);
        var iterable = lastResult.fetchAndReset();

        if (iterable instanceof Map<?,?> iterableDict) {
            iterableDict.keySet().forEach(key -> {

                callStack.peek().addScope();
                callStack.peek().declareVar(new Variable(
                        foreachStatement.varName(),
                        foreachStatement.varType(),
                        key
                ));

                foreachStatement.statementBlock().accept(this);

                callStack.peek().removeScope();
            });
        }
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
            List<Variable> arguments = new ArrayList<>();

            for(int idx = 0; idx < functionCall.arguments().size(); idx++) {
                functionCall.arguments().get(idx).accept(this);

                arguments.add(new Variable(
                        functionDef.parameters().get(idx).name(),
                        functionDef.parameters().get(idx).type(),
                        lastResult.fetchAndReset()
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

                arguments.add(new Variable("arg" + idx, new StringType(), lastResult.fetchAndReset()));
            }

            callStack.push(new FunctionCallContext(
                    functionCall.functionName(),
                    new Scope(arguments),
                    globalScope
            ));
            builtinFunctionDef.run();
            callStack.pop();
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
        dictValue.dict().accept(this);
        Object dict = lastResult.fetchAndReset();

        dictValue.key().accept(this);
        Object key = lastResult.fetchAndReset();

        if (dict instanceof LinkedHashMap<?,?>) {
            var value = ((LinkedHashMap<?, ?>) dict).get(key);

            if (value == null) {
                throw new AppNullPointerException(String.format("Key '%s' doesn't exist", key.toString()));
            }

            lastResult.store(value);
        }
    }

    @Override
    public void visit(DictLiteral dictLiteral) {
        Map<Object, Object> dictContent = new LinkedHashMap<>();

        dictLiteral.content().forEach((k, v) -> {
            k.accept(this);
            Object key = lastResult.fetchAndReset();

            v.accept(this);
            Object value = lastResult.fetchAndReset();

            dictContent.put(key, value);
        });

        lastResult.store(dictContent);
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

        assertNotNull(left);
        assertNotNull(right);

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
        } catch (AppNullPointerException e) {
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

        lastResult.store(booleanToInteger(left.equals(right)));
    }

    @Override
    public void visit(NotEqual notEqual) {
        notEqual.left().accept(this);
        var left = lastResult.fetchAndReset();

        notEqual.right().accept(this);
        var right = lastResult.fetchAndReset();

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

        assertNotNull(value);

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

        context.declareVar(new Variable(
                variableDeclaration.name(),
                variableDeclaration.type(),
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

    private void executeBuiltinPrint() {
        var context = callStack.peek();
        Variable arg0 = context.findVar("arg0").orElseThrow();

        System.out.println((String) arg0.getValue());
    }

    private void executeBuiltinInput() {
        var scanner = new Scanner(System.in);
        lastResult.store(scanner.nextLine());
    }

    private void assertNotNull(Object value) {
        if (value.equals(Null.getInstance())) {
            throw new AppNullPointerException("Unexpected null");
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

    public static class AppRuntimeException extends RuntimeException {
        public AppRuntimeException(String message) {
            super(message);
        }
    }

    public static class AppNullPointerException extends AppRuntimeException {
        AppNullPointerException(String message) {
            super(message);
        }
    }
}
