package ast;

import ast.statement.VariableDeclaration;

import java.util.HashMap;
import java.util.Map;

public record Program(Map<String, FunctionDefinition> functions, Map<String, VariableDeclaration> globalVariables) implements AstNode {
    public Program(Map<String, FunctionDefinition> functions, Map<String, VariableDeclaration> globalVariables) {
        this.functions = new HashMap<>(functions);
        this.globalVariables = new HashMap<>(globalVariables);
    }

    @Override
    public void accept(AstVisitor visitor) {
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
