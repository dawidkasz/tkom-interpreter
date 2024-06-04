package executor;

import ast.expression.Null;
import ast.type.DictType;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.Type;

import java.util.HashMap;

final class Variable {
    private final String name;
    private final Type type;
    private Object value;

    Variable(String name, Type type) {
        this(name, type, Null.getInstance());
    }

    Variable(String name, Type type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
