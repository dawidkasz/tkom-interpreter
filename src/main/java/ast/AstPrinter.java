package ast;

import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DictKeyValue;
import ast.expression.DictLiteral;
import ast.expression.DivideExpression;
import ast.expression.FloatLiteral;
import ast.expression.IntLiteral;
import ast.expression.LessThanExpression;
import ast.expression.MinusExpression;
import ast.expression.ModuloExpression;
import ast.expression.MultiplyExpression;
import ast.expression.NegatedExpression;
import ast.expression.Null;
import ast.expression.OrExpression;
import ast.statement.ReturnStatement;
import ast.statement.WhileStatement;

public class AstPrinter implements Visitor {
    private static final int INDENTATION_LEVEL = 2;

    private int indentation = 0;

    @Override
    public void visit(Program program) {
        print("Program [\n");
        withIndentation(() ->
            program.functions().forEach((funName, funDef) -> {
                funDef.accept(this);
                print("\n");
            })
        );
        print("]");
    }

    @Override
    public void visit(FunctionDefinition def) {
        print("Function [\n");
        withIndentation(() -> {
            print(String.format("name=%s\n", def.name()));
            print(String.format("returnType=%s\n", def.returnType()));
            print("parameters=[");
            withIndentation(() ->
                def.parameters().forEach(param -> {
                    print("\n");
                    print(String.format("Param [name=%s type=%s]", param.name(), param.type()));
                })
            );

            if (def.parameters().isEmpty()) {
                System.out.print("]\n");
            } else {
                print("\n");
                print("]\n");
            }

            print("body=[");
            withIndentation(() ->
                    def.statementBlock().forEach(statement -> {
                        print("\n");
                        statement.accept(this);
                    })
            );
            if (def.statementBlock().isEmpty()) {
                System.out.print("]");
            } else {
                print("\n");
                print("]");
            }
        });
        print("\n");
        print("]");
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        print("While [\n");
        withIndentation(() -> {
            print("condition=\n");
            withIndentation(() -> whileStatement.condition().accept(this));
            print("\n");
            print("body=[");
            withIndentation(() ->
                    whileStatement.statementBlock().forEach(statement -> {
                        print("\n");
                        statement.accept(this);
                    })
            );
            if (whileStatement.statementBlock().isEmpty()) {
                System.out.print("]");
            } else {
                print("\n");
                print("]");
            }
        });
        print("\n");
        print("]");
    }

    @Override
    public void visit(ReturnStatement returnStatement) {
        print("Return [\n");
        withIndentation(() -> {
            print("value=\n");
            withIndentation(() -> returnStatement.expression().accept(this));
            print("\n");
        });
        print("]");
    }

    @Override
    public void visit(FunctionCall functionCall) {

    }

    @Override
    public void visit(DivideExpression divideExpression) {

    }

    @Override
    public void visit(CastedExpression castedExpression) {

    }

    @Override
    public void visit(DictKeyValue dictKeyValue) {

    }

    @Override
    public void visit(DictLiteral dictLiteral) {

    }

    @Override
    public void visit(AndExpression andExpression) {

    }

    @Override
    public void visit(FloatLiteral floatLiteral) {

    }

    @Override
    public void visit(IntLiteral intLiteral) {
        print(String.format("LITERAL[%s]", intLiteral.value()));
    }

    @Override
    public void visit(LessThanExpression lessThanExpression) {

    }

    @Override
    public void visit(MinusExpression minusExpression) {

    }

    @Override
    public void visit(ModuloExpression moduloExpression) {

    }

    @Override
    public void visit(MultiplyExpression multiplyExpression) {

    }

    @Override
    public void visit(NegatedExpression negatedExpression) {

    }

    @Override
    public void visit(Null aNull) {

    }

    @Override
    public void visit(OrExpression orExpression) {
        print("OR [\n");
        withIndentation(() -> {
            orExpression.left().accept(this);
            print("\n");
            orExpression.right().accept(this);
            print("\n");
        });
        print("]");
    }

    private void withIndentation(Runnable func) {
        indentation += INDENTATION_LEVEL;
        func.run();
        indentation -= INDENTATION_LEVEL;
    }

    private void print(String value) {
        System.out.print(" ".repeat(indentation) + value);
    }
}
