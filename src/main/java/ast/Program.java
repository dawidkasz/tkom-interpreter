package ast;

import java.util.HashMap;
import java.util.Map;

public record Program(Map<String, FunctionDefinition> functions) implements Visitable {
    public Program(Map<String, FunctionDefinition> functions) {
        this.functions = new HashMap<>(functions);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
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
