package ast.expression;

import ast.Visitor;

public record DictValue(Expression dict, Expression key) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
