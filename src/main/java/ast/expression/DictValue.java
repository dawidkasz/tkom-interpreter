package ast.expression;

import ast.AstVisitor;

public record DictValue(Expression dict, Expression key) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
