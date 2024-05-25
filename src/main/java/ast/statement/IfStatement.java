package ast.statement;

import ast.AstVisitor;
import ast.expression.Expression;
import lexer.Position;

import java.util.List;

public record IfStatement(
        Expression condition,
        List<Statement> ifBlock,
        List<Statement> elseBlock,
        Position position
) implements Statement {
    public IfStatement(Expression condition, List<Statement> ifBlock, Position position) {
        this(condition, ifBlock, null, position);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
