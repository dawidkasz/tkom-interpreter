package ast.statement;

import ast.AstVisitor;
import ast.expression.Expression;
import ast.type.SimpleType;
import lexer.Position;

import java.util.List;

public record ForeachStatement(
        SimpleType varType, String varName, Expression iterable, List<Statement> statementBlock, Position position
) implements Statement {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
