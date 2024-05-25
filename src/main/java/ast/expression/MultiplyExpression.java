package ast.expression;

import ast.AstVisitor;

public record MultiplyExpression(Expression left, Expression right) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
