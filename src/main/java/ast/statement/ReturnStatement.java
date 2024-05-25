package ast.statement;

import ast.AstVisitor;
import ast.expression.Expression;
import lexer.Position;

public record ReturnStatement(Expression expression, Position position) implements Statement {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
