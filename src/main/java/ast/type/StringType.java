package ast.type;

public record StringType() implements SimpleType {
    @Override
    public String toString() {
        return "string";
    }
}
