package ast.expression;

import ast.Visitor;

public final class Null implements Expression {
    public static final Null instance = new Null();

    public static Null getInstance() {
        return instance;
    }

    private Null() {}

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
