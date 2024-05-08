package ast;

import ast.statement.Statement;
import ast.type.Type;
import lexer.Position;

import java.util.List;

public record FunctionDefinition(
        Type returnType,
        String name,
        List<Parameter> parameters,
        List<Statement> statementBlock,
        Position position
) implements Visitable, Declaration {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
