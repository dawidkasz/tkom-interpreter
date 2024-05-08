package ast.type;

public record VoidType() implements Type {
    @Override
    public String toString() {
        return "void";
    }
}
