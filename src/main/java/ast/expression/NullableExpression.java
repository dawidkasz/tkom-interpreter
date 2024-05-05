package ast.expression;

public class NullableExpression implements Expression {
    private final Expression expression;

    public NullableExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return String.format("NULLABLE[%s]", expression);
    }
}
