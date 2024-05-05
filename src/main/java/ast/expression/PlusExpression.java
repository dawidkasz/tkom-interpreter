package ast.expression;

import ast.Visitor;

public class PlusExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public PlusExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("(%s + %s)", left, right);
    }

    @Override
    public void accept(Visitor visitor) {

    }
}
