package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import lexer.Position;

public record ReturnStatement(Expression expression, Position position) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
