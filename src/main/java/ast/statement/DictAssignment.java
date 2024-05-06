package ast.statement;

import ast.Visitor;
import ast.expression.Expression;

public record DictAssignment(String variableName, Expression key, Expression value) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
