package ast.expression;

import ast.AstVisitor;

public record VariableValue(String varName) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
