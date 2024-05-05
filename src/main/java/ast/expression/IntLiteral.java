package ast.expression;

public class IntLiteral implements Expression {
    private final int value;

    public IntLiteral(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
