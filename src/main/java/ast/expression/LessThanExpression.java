package ast.expression;

public class LessThanExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public LessThanExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("(%s < %s)", left, right);
    }
}
