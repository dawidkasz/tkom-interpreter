package ast.expression;

import ast.Visitor;

public record IntLiteral(int value) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
