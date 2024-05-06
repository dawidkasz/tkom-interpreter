package ast.type;

public record IntType() implements SimpleType {
    @Override
    public String toString() {
        return "int";
    }
}
