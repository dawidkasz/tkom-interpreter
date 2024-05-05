package ast;

import ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public class FunctionDefinition {
    private final String type;
    private final String name;
    private final List<Parameter> parameters;
    private final List<Statement> block;

    public FunctionDefinition(String name, String def, List<Parameter> parameters, List<Statement> block) {
        this.type = name;
        this.name = def;
        this.parameters = parameters;
        this.block = block;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        block.forEach(s -> {
            builder.append(s.toString());
            builder.append("\n");
        });

        List<String> parametersStr = new ArrayList<>();
        parameters.forEach(p -> parametersStr.add(p.toString()));


        return String.format("%s %s (%s) { \n %s }", type, name, String.join(", ", parametersStr), builder);
    }
}
