package ast.expression;

import ast.AstVisitor;

public record NullableExpression(Expression expression) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
