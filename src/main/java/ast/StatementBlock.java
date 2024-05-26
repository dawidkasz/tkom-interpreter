package ast;

import ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public record StatementBlock(List<Statement> statements) implements AstNode {
    public StatementBlock(List<Statement> statements) {
        this.statements = new ArrayList<>(statements);
    }

    public boolean isEmpty() {
        return statements.isEmpty();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
