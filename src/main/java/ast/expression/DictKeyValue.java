package ast.expression;

import ast.Visitor;

public record DictKeyValue(Expression dict, Expression key) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
