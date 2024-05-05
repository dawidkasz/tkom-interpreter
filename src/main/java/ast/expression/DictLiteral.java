package ast.expression;

import ast.Visitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record DictLiteral(Map<Expression, Expression> content) implements Expression {
    public DictLiteral(Map<Expression, Expression> content) {
        this.content = new HashMap<>(content);
    }

    public static DictLiteral empty() {
        return new DictLiteral(Collections.emptyMap());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
