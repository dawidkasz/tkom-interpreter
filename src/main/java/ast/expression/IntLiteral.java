package ast.expression;

import ast.AstVisitor;

public record IntLiteral(int value) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
