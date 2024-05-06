package ast.type;

public record FloatType() implements SimpleType {
    @Override
    public String toString() {
        return "float";
    }
}
