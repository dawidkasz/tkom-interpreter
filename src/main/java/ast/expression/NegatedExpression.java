package ast.expression;

import ast.Visitor;

public class NegatedExpression implements Expression {
    private final Expression expression;

    public NegatedExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return String.format("![%s]", expression);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
