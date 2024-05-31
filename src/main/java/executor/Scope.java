package executor;

import ast.expression.Null;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

final class Scope {
    private final Map<String, Variable> variables = new HashMap<>();

    Scope() {
    }

    Scope(List<Variable> vars) {
        vars.forEach(var -> variables.put(var.getName(), var));
    }

    public void declareVar(Variable newVar) {
        if (variables.containsKey(newVar.getName())) {
            throw new IllegalArgumentException(
                    String.format("Variable %s already defined", newVar.getName()));
        }

        variables.put(newVar.getName(), newVar);
    }

    public void assignVar(String varName, Object value) {
        Variable var = Optional.ofNullable(variables.get(varName))
                .orElseThrow(() -> new IllegalArgumentException("Variable " + varName + " is not defined"));

        if (var.getType() != value.getClass() && !value.equals(Null.getInstance())) {
            throw new IllegalArgumentException("Can't assign variable, because types don't match");
        }

        var.setValue(value);
    }

    public Optional<Variable> get(String varName) {
        return Optional.ofNullable(variables.get(varName));
    }

    public boolean contains(String varName) {
        return variables.containsKey(varName);
    }
}
