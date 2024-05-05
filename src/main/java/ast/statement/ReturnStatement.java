package ast.statement;

import ast.expression.Expression;

public class ReturnStatement implements Statement {
    private final Expression expression;

    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return String.format("return %s", expression);
    }
}
