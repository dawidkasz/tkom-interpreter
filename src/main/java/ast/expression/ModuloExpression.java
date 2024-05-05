package ast.expression;

import ast.Visitor;

public class ModuloExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public ModuloExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, "%", right);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
