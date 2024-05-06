package ast.expression;

import ast.Visitor;

public record LessThanExpression(Expression left, Expression right) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
