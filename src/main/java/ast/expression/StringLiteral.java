package ast.expression;

import ast.Visitor;

public class StringLiteral implements Expression {
    private final String value;

    public StringLiteral(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", value);
    }

    @Override
    public void accept(Visitor visitor) {

    }
}
