package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import ast.type.SimpleType;
import lexer.Position;

import java.util.List;

public record ForeachStatement(
        SimpleType varType, String varName, Expression iterable, List<Statement> statementBlock, Position position
) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
