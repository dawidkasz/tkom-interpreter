package ast;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Program {
    private final Map<String, FunctionDefinition> functions;

    public Program(Map<String, FunctionDefinition> functions) {
        this.functions = new HashMap<>(functions);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        functions.forEach((key, value) -> {
            builder.append(value.toString());
            builder.append("\n\n");
        });

        return builder.toString();
    }
}
