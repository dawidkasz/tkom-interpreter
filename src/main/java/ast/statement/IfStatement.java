package ast.statement;

import ast.Visitor;
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
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
