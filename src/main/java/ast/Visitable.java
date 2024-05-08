package ast;

public interface Visitable {
    void accept(Visitor visitor);
}
