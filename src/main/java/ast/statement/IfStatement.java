package ast.statement;

import ast.Visitor;
import ast.expression.Expression;

import java.util.Collections;
import java.util.List;

public record IfStatement(
        Expression condition,
        List<Statement> ifBlock,
        List<Statement> elseBlock
) implements Statement {
    public IfStatement(Expression condition, List<Statement> ifBlock) {
        this(condition, ifBlock, Collections.emptyList());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
