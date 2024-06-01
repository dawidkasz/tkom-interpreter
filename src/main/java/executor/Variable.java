package executor;

import ast.expression.Null;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.Type;
import com.sun.jdi.LongType;

final class Variable {
    private final String name;
    private final Type type;
    private Object value;

    Variable(String name, Type type) {
        this(name, type, Null.getInstance());
    }

    Variable(String name, Type type, Object value) {
        if (!value.equals(Null.getInstance()) && !Variable.getProgramType(value.getClass()).equals(type)) {
            throw new IllegalArgumentException(String.format("Value %s is not a valid %s", value, type));
        }

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
        if (
                !value.equals(Null.getInstance()) &&
                !Variable.getProgramType(value.getClass()).equals(type)
        ) {
            throw new IllegalArgumentException(String.format("Value %s is not a valid %s, %s, %s",
                    value, type, Variable.getProgramType(value.getClass()), value.getClass()));
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

    public static Type getProgramType(Class<?> clazz) {
        if (clazz.equals(Integer.class)) {
            return new IntType();
        }

        if (clazz.equals(Float.class)) {
            return new FloatType();
        }

        if (clazz.equals(String.class)) {
            return new StringType();
        }

        throw new IllegalArgumentException("Unknown type " + clazz);
    }
}
