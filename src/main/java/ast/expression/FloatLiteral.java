package ast.expression;

import ast.AstVisitor;

public record FloatLiteral(float value) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
