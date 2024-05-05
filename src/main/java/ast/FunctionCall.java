package ast;

import ast.expression.Expression;
import ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall implements Statement, Expression {
    private final String functionName;
    private final List<Expression> arguments;

    public FunctionCall(String functionName, List<Expression> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        List<String> args = new ArrayList<>();
        arguments.forEach(arg -> args.add(arg.toString()));

        return String.format("%s(%s)", functionName, String.join(", ", args));
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
