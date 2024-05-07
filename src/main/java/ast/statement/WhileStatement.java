package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import lexer.Position;

import java.util.List;

public record WhileStatement(Expression condition, List<Statement> statementBlock, Position position) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
