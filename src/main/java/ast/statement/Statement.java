package ast.statement;

import ast.AstNode;
import lexer.Position;

public interface Statement extends AstNode {
    Position position();
}
