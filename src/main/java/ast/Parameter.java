package ast;

public record Parameter(String type, String name) {
    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }
}
