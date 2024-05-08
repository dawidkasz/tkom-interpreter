package ast.statement;

import ast.Visitable;
import lexer.Position;

public interface Statement extends Visitable {
    Position position();
}
