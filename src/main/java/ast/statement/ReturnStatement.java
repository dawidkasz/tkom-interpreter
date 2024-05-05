package ast.statement;

import ast.Visitor;
import ast.expression.Expression;

public record ReturnStatement(Expression expression) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
