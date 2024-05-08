package ast.expression;

import ast.Visitor;

public record StringLiteral(String value) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
