package ast.expression;

import ast.Visitor;

public class FloatLiteral implements Expression {
    private final float value;

    public FloatLiteral(float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
