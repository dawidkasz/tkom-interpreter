package ast;

import ast.type.Type;
import lexer.Position;

import java.util.List;

public record FunctionDefinition(
        Type returnType,
        String name,
        List<Parameter> parameters,
        StatementBlock statementBlock,
        Position position
) implements AstNode, Declaration {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
