package executor;

import ast.expression.Null;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.Type;
import com.sun.jdi.LongType;

final class Variable {
    private final String name;
    private final Class<?> type;
    private Object value;

    Variable(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    Variable(String name, Class<?> type, Object value) {
        if (!value.getClass().equals(type) && !value.equals(Null.getInstance())) {
            throw new IllegalArgumentException(String.format("Value %s is not a valid %s", value, type));
        }

        this.name = name;
        this.type = type;
        this.value = value;
    }

    public void setValue(Object value) {
        if (!value.getClass().equals(type) && !value.equals(Null.getInstance())) {
            throw new IllegalArgumentException(String.format("Value %s is not a valid %s", value, type));
        }

        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public static Class<?> getInterpreterType(Type type) {
        if (type instanceof IntType) {
            return Integer.class;
        }

        if (type instanceof FloatType) {
            return Float.class;
        }

        if (type instanceof StringType) {
            return String.class;
        }

        throw new RuntimeException();
    }
}
