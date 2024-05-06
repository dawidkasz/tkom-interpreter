package ast.expression;

import ast.Visitor;

public record FloatLiteral(float value) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
