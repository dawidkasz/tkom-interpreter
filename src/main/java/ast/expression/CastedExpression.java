package ast.expression;

import ast.Visitor;

public record CastedExpression(Expression expression, String asType) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
