package ast.expression;

import ast.AstVisitor;

public final class Null implements Expression {
    private static final Null instance = new Null();

    public static Null getInstance() {
        return instance;
    }

    private Null() {}

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
