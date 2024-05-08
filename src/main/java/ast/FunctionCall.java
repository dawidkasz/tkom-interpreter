package ast;

import ast.expression.Expression;
import ast.statement.Statement;
import lexer.Position;

import java.util.ArrayList;
import java.util.List;

public record FunctionCall(String functionName, List<Expression> arguments, Position position) implements Statement, Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
