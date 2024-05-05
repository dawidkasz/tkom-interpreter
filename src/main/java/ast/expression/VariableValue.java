package ast.expression;

import ast.Visitor;

public class VariableValue implements Expression {
    private final String variableName;

    public VariableValue(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String toString() {
        return variableName;
    }

    @Override
    public void accept(Visitor visitor) {

    }
}
