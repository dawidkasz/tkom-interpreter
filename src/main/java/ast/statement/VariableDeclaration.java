package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import ast.type.Type;

public record VariableDeclaration(Type type, String name, Expression value) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
