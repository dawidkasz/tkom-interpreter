package ast;

import ast.expression.Expression;

public class Variable{
    private final String name;
    private final Expression value;

    public Variable(String name, Expression value) {
        this.name = name;
        this.value = value;
    }

    public Variable(String name) {
        this(name, null);
    }
}
