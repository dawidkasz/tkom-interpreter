package ast.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictLiteral implements Expression {
    private final Map<Expression, Expression> content;

    public DictLiteral(Map<Expression, Expression> content) {
        this.content = new HashMap<>(content);
    }

    public static DictLiteral empty() {
        return new DictLiteral(Collections.emptyMap());
    }

    @Override
    public String toString() {
        List<String> params = new ArrayList<>();
        content.forEach((k, v) -> {
            params.add(String.format("%s: %s", k, v));
        });

        return "{" + String.join(", ", params) + "}";
    }
}
