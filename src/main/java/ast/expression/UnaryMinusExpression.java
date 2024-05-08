package ast.expression;

import ast.Visitor;

public record UnaryMinusExpression(Expression expression) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
