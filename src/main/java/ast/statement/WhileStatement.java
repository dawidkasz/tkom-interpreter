package ast.statement;

import ast.AstVisitor;
import ast.StatementBlock;
import ast.expression.Expression;
import lexer.Position;

import java.util.List;

public record WhileStatement(Expression condition, StatementBlock statementBlock, Position position) implements Statement {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
