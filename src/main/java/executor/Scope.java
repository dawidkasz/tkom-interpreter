package executor;

import java.util.HashMap;
import java.util.Map;

final class Scope {
    private final Map<String, Object> variables;

    Scope() {
        variables = new HashMap<>();
    }

    Scope(Map<String, Object> variables) {
        this.variables = new HashMap<>(variables);
    }

    public void assign(String varName, Object value) {
        variables.put(varName, value);
    }

    public Object get(String varName) {
        return variables.get(varName);
    }

    public boolean contains(String varName) {
        return variables.containsKey(varName);
    }

    static class Builder {
        private final Map<String, Object> variables = new HashMap<>();

        Builder var(String name, Object value) {
            variables.put(name, value);
            return this;
        }

        Scope build() {
            return new Scope(variables);
        }
    }
}
