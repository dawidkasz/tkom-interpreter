package ast;

import ast.statement.Statement;

import java.util.List;

public record FunctionDefinition(
        String returnType, String name, List<Parameter> parameters, List<Statement> statementBlock) implements Visitable {

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
