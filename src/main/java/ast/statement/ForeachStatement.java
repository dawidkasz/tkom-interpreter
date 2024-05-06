package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import ast.type.SimpleType;

import java.util.List;

public record ForeachStatement(
        SimpleType varType, String varName, Expression iterable, List<Statement> statementBlock
) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
