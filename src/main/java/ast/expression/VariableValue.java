package ast.expression;

public class VariableValue implements Expression {
    private final String variableName;

    public VariableValue(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String toString() {
        return variableName;
    }
}
