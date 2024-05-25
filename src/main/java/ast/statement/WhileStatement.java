package ast.statement;

import ast.AstVisitor;
import ast.expression.Expression;
import lexer.Position;

import java.util.List;

public record WhileStatement(Expression condition, List<Statement> statementBlock, Position position) implements Statement {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
