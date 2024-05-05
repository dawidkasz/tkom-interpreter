package ast.expression;

import ast.Visitor;

public class NullableExpression implements Expression {
    private final Expression expression;

    public NullableExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return String.format("NULLABLE[%s]", expression);
    }

    @Override
    public void accept(Visitor visitor) {

    }
}
