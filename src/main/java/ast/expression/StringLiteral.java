package ast.expression;

import ast.AstVisitor;

public record StringLiteral(String value) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
