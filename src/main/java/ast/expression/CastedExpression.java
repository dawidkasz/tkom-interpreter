package ast.expression;

public class CastedExpression implements Expression {
    private final Expression expression;
    private final String asType;

    public CastedExpression(Expression expression, String asType) {
        this.expression = expression;
        this.asType = asType;
    }

    @Override
    public String toString() {
        return String.format("[%s as %s]", expression, asType);
    }
}
