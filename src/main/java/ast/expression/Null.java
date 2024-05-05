package ast.expression;

public final class Null implements Expression {
    public static final Null instance = new Null();

    public static Null getInstance() {
        return instance;
    }

    private Null() {}

    @Override
    public String toString() {
        return "null";
    }
}
