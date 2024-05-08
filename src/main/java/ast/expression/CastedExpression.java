package ast.expression;

import ast.Visitor;
import ast.type.Type;

public record CastedExpression(Expression expression, Type asType) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
