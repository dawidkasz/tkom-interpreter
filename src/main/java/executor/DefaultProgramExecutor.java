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
import ast.statement.VariableAssignment;
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import com.sun.jdi.IntegerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;

public class DefaultProgramExecutor implements AstVisitor, ProgramExecutor {
    private final Stack<FunctionCallContext> callStack = new Stack<>();
    private final Scope globalScope = new Scope();
    private final LastResult lastResult = new LastResult();
    private final Map<String, FunctionDefinition> functions = new HashMap<>();
    private final Map<String, Runnable> builtinFunctions;

    public DefaultProgramExecutor() {
        builtinFunctions = Map.of(
                "print", this::executeBuiltinPrint
        );
    }

    private void executeBuiltinPrint() {
        var context = callStack.peek();
        Object value = context.findVar("arg0");

        if (!value.getClass().equals(String.class)) {
            throw new RuntimeException("Invalid print arg");
        }

        System.out.println((String) value);

        callStack.pop();
    }

    @Override
    public void execute(Program program) {
        visit(program);
    }

    @Override
    public void visit(Program program) {
        functions.putAll(program.functions());

        program.globalVariables().forEach((varName, varDec) -> {
            varDec.value().accept(this);
            globalScope.assign(varName, lastResult.fetchAndReset());
        });

        callStack.push(new FunctionCallContext("main"));
        program.functions().get("main").statementBlock().accept(this);
    }

    @Override
    public void visit(FunctionDefinition functionDefinition) {
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {

    }

    @Override
    public void visit(DictAssignment dictAssignment) {

    }

    @Override
    public void visit(WhileStatement whileStatement) {

    }

    @Override
    public void visit(ForeachStatement foreachStatement) {

    }

    @Override
    public void visit(IfStatement ifStatement) {

    }

    @Override
    public void visit(ReturnStatement returnStatement) {

    }

    @Override
    public void visit(FunctionCall functionCall) {
        FunctionDefinition functionDef = functions.get(functionCall.functionName());
        if (functionDef != null) {
            return;
        }

        Runnable builtinFunctionDef = builtinFunctions.get(functionCall.functionName());
        if (builtinFunctionDef != null) {
            Map<String, Object> arguments = new HashMap<>();

            for(int idx = 0; idx < functionCall.arguments().size(); idx++) {
                functionCall.arguments().get(idx).accept(this);
                arguments.put("arg" + idx, lastResult.fetchAndReset());
            }

            callStack.push(new FunctionCallContext(functionCall.functionName(), new Scope(arguments)));
            builtinFunctionDef.run();
            return;
        }

        throw new RuntimeException("Unrecognized function name " + functionCall.functionName());
    }

    @Override
    public void visit(DivideExpression divideExpression) {

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
    public void visit(AndExpression andExpression) {

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
    public void visit(LessThan lessThan) {

    }

    @Override
    public void visit(MinusExpression minusExpression) {
        minusExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        minusExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left - (Integer) right);
        } else if (validateTypes(left, right, Float.class)) {
            lastResult.store((Float) left - (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(ModuloExpression moduloExpression) {

    }

    @Override
    public void visit(MultiplyExpression multiplyExpression) {
        multiplyExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        multiplyExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        if (validateTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left * (Integer) right);
        } else if (validateTypes(left, right, Float.class)) {
            lastResult.store((Float) left * (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }

    @Override
    public void visit(NegationExpression negationExpression) {

    }

    @Override
    public void visit(Null aNull) {
        lastResult.store(Null.getInstance());
    }

    @Override
    public void visit(OrExpression orExpression) {

    }

    @Override
    public void visit(VariableValue variableValue) {
        var value = callStack.peek().findVar(variableValue.varName());
        lastResult.store(value);
    }

    @Override
    public void visit(PlusExpression plusExpression) {
        plusExpression.left().accept(this);
        var left = lastResult.fetchAndReset();

        plusExpression.right().accept(this);
        var right = lastResult.fetchAndReset();

        assertNotNull(left);
        assertNotNull(right);

        if (validateTypes(left, right, Integer.class)) {
            lastResult.store((Integer) left + (Integer) right);
        } else if (validateTypes(left, right, String.class)) {
            lastResult.store((String) left + (String) right);
        } else if (validateTypes(left, right, Float.class)) {
            lastResult.store((Float) left + (Float) right);
        } else {
            throw new RuntimeException("types do not match");
        }
    }



    private boolean validateTypes(Object o1, Object o2, Class<?> clazz) {
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
    public void visit(LessThanOrEqual lessThanOrEqual) {

    }

    @Override
    public void visit(GreaterThan greaterThan) {

    }

    @Override
    public void visit(GreaterThanOrEqual greaterThanOrEqual) {

    }

    @Override
    public void visit(Equal equal) {

    }

    @Override
    public void visit(NotEqual notEqual) {

    }

    @Override
    public void visit(UnaryMinusExpression unaryMinusExpression) {

    }

    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        FunctionCallContext context = callStack.peek();

        variableDeclaration.value().accept(this);
        Object value = lastResult.fetchAndReset();

        if (
                value.getClass().equals(Integer.class) && !variableDeclaration.type().equals(new IntType()) ||
                value.getClass().equals(String.class) && !variableDeclaration.type().equals(new StringType()) ||
                value.getClass().equals(Float.class) && !variableDeclaration.type().equals(new FloatType())
        ) {
            throw new RuntimeException("Types do not match");
        }


        context.setVar(variableDeclaration.name(), value);
    }

    @Override
    public void visit(StatementBlock statementBlock) {
        statementBlock.statements().forEach(statement -> statement.accept(this));
    }

    private void assertNotNull(Object value) {
        if (value.equals(Null.getInstance())) {
            throw new NullException();
        }
    }

    public static class NullException extends RuntimeException {
    }
}
