package ast.statement;

import ast.AstVisitor;
import ast.expression.Expression;
import lexer.Position;

public record DictAssignment(String variableName, Expression key, Expression value, Position position) implements Statement {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
