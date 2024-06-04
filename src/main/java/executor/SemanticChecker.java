package executor;

import ast.Program;

public interface SemanticChecker {
    void verify(Program program);
}
