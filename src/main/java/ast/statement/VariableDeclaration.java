package ast.statement;

import ast.Declaration;
import ast.Visitor;
import ast.expression.Expression;
import ast.type.Type;
import lexer.Position;

public record VariableDeclaration(Type type, String name, Expression value, Position position) implements Statement, Declaration {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
