package ast;

import ast.expression.Expression;
import ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public record FunctionCall(String functionName, List<Expression> arguments) implements Statement, Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
