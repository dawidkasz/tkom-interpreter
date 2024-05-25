package ast.expression;

import ast.AstVisitor;
import ast.type.Type;

public record CastedExpression(Expression expression, Type asType) implements Expression {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
