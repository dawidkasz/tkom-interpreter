package ast.expression;

import ast.Visitor;

public record NullableExpression(Expression expression) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
