package ast.statement;

import ast.Visitor;
import ast.expression.Expression;
import ast.type.Type;
import lexer.Position;

public record VariableDeclaration(Type type, String name, Expression value, Position position) implements Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
