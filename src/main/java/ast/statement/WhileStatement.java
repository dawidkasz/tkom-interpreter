package ast.statement;

import ast.expression.Expression;

import java.util.List;

public class WhileStatement implements Statement {
    private final Expression expression;
    private final List<Statement> block;

    public WhileStatement(Expression expression, List<Statement> block) {
        this.expression = expression;
        this.block = block;
    }

    @Override
    public String toString() {
        var statements = new StringBuilder();
        block.forEach(s -> statements.append(s.toString()));

        return String.format("while %s { \n %s \n }", expression, statements);
    }
}
