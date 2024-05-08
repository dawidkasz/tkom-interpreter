package ast.expression;

import ast.Visitor;

public record VariableValue(String varName) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
