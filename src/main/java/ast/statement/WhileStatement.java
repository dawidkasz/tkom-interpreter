package ast.statement;

import ast.Visitor;
import ast.expression.Expression;

import java.util.List;

public record WhileStatement(Expression condition, List<Statement> statementBlock) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
