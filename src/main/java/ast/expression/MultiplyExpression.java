package ast.expression;

import ast.Visitor;

public record MultiplyExpression(Expression left, Expression right) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
