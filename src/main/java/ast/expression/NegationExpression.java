package ast.expression;

import ast.AstVisitor;

public record NegationExpression(Expression expression) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
