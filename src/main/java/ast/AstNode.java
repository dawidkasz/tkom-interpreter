package ast;

public interface AstNode {
    void accept(AstVisitor visitor);
}
