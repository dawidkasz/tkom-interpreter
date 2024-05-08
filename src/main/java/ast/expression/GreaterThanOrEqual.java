package ast.expression;

import ast.Visitor;

public record GreaterThanOrEqual(Expression left, Expression right) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
