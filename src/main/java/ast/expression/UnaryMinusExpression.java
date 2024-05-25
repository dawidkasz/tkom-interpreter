package ast.expression;

import ast.AstVisitor;

public record UnaryMinusExpression(Expression expression) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
