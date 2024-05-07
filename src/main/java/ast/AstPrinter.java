package ast;

import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DictValue;
import ast.expression.DictLiteral;
import ast.expression.DivideExpression;
import ast.expression.Equal;
import ast.expression.Expression;
import ast.expression.FloatLiteral;
import ast.expression.GreaterThan;
import ast.expression.GreaterThanOrEqual;
import ast.expression.IntLiteral;
import ast.expression.LessThan;
import ast.expression.LessThanOrEqual;
import ast.expression.MinusExpression;
import ast.expression.ModuloExpression;
import ast.expression.MultiplyExpression;
import ast.expression.NegationExpression;
import ast.expression.NotEqual;
import ast.expression.Null;
import ast.expression.NullableExpression;
import ast.expression.OrExpression;
import ast.expression.PlusExpression;
import ast.expression.StringLiteral;
import ast.expression.UnaryMinusExpression;
import ast.expression.VariableValue;
import ast.statement.DictAssignment;
import ast.statement.ForeachStatement;
import ast.statement.IfStatement;
import ast.statement.ReturnStatement;
import ast.statement.VariableAssignment;
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;

public class AstPrinter implements Visitor {
    private static final int INDENTATION_LEVEL = 2;

    private int indentation = 0;
    private boolean nextPrintWithoutIndentation = false;

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
                    print(String.format("Param(name=%s type=%s)", param.name(), param.type()));
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
    public void visit(VariableAssignment assignment) {
        print("Assign(\n");
        withIndentation(() -> {
            print(String.format("varName=%s\n", assignment.variableName()));
            print("value=");
            withoutIndentationForNextLine(() -> assignment.expression().accept(this));
        });
        print("\n");
        print(")");
    }

    @Override
    public void visit(DictAssignment assignment) {
        print("DictAssign(\n");
        withIndentation(() -> {
            print(String.format("varName=%s\n", assignment.variableName()));
            print("key=");
            withoutIndentationForNextLine(() -> assignment.key().accept(this));
            print("\n");
            print("value=");
            withoutIndentationForNextLine(() -> assignment.value().accept(this));
        });
        print("\n");
        print(")");
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        print("While(\n");
        withIndentation(() -> {
            print("condition=");
            withoutIndentationForNextLine(() -> whileStatement.condition().accept(this));
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
    public void visit(ForeachStatement foreachStatement) {
        print("Foreach(\n");
        withIndentation(() -> {
            print(String.format("varType=%s\n", foreachStatement.varType()));
            print(String.format("varName=%s\n", foreachStatement.varName()));
            print("iterable=");
            withoutIndentationForNextLine(() -> foreachStatement.iterable().accept(this));
            print("\n");
            print("body=[");
            withIndentation(() ->
                    foreachStatement.statementBlock().forEach(statement -> {
                        print("\n");
                        statement.accept(this);
                    })
            );
            if (foreachStatement.statementBlock().isEmpty()) {
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
    public void visit(IfStatement ifStatement) {
        print("If(\n");
        withIndentation(() -> {
            print("condition=");
            withoutIndentationForNextLine(() -> ifStatement.condition().accept(this));
            print("\n");
            print("ifBody=[");
            withIndentation(() ->
                    ifStatement.ifBlock().forEach(statement -> {
                        print("\n");
                        statement.accept(this);
                    })
            );
            if (ifStatement.ifBlock().isEmpty()) {
                System.out.print("]");
            } else {
                print("\n");
                print("]");
            }

            if (ifStatement.elseBlock() != null) {
                print("\n");
                print("elseBody=[");
                withIndentation(() ->
                        ifStatement.elseBlock().forEach(statement -> {
                            print("\n");
                            statement.accept(this);
                        })
                );
                if (ifStatement.elseBlock().isEmpty()) {
                    System.out.print("]");
                } else {
                    print("\n");
                    print("]");
                }
            }
        });
        print("\n");
        print("]");
    }

    @Override
    public void visit(ReturnStatement returnStatement) {
        print("Return(\n");
        withIndentation(() -> {
            print("value=");
            withoutIndentationForNextLine(() ->  returnStatement.expression().accept(this));
            print("\n");
        });
        print(")");
    }

    @Override
    public void visit(FunctionCall functionCall) {
        print("Call(\n");
        withIndentation(() -> {
            print(String.format("name=%s\n", functionCall.functionName()));

            for (int argIdx = 0; argIdx < functionCall.arguments().size(); ++argIdx) {
                int currentArgIdx = argIdx;

                print(String.format("arg%s=", argIdx));
                withoutIndentationForNextLine(
                        () -> functionCall.arguments().get(currentArgIdx).accept(this)
                );
                print("\n");
            }
        });

        print(")");
    }

    @Override
    public void visit(DivideExpression divideExpression) {
        visitBinOp("Div", divideExpression.left(), divideExpression.right());
    }

    @Override
    public void visit(CastedExpression castedExpression) {
        print("As(\n");
        withIndentation(() -> {
            print("value=");
            withoutIndentationForNextLine(
                    () -> castedExpression.expression().accept(this)
            );
            print("\n");
            print(String.format("asType=%s", castedExpression.asType()));
            print("\n");
        });
        print(")");
    }

    @Override
    public void visit(DictValue dictValue) {
        print("DictValue(\n");
        withIndentation(() -> {
            print("dict=");
            withoutIndentationForNextLine(() -> dictValue.dict().accept(this));
            print("\n");
            print("key=");
            withoutIndentationForNextLine(() -> dictValue.key().accept(this));
            print("\n");
        });
        print(")");
    }

    @Override
    public void visit(DictLiteral dictLiteral) {
        print("Lit({");

        int processedKeys = 0;

        for (var entry : dictLiteral.content().entrySet()) {
            withoutIndentationForNextLine(() -> entry.getKey().accept(this));
            System.out.print(": ");
            withoutIndentationForNextLine(() -> entry.getValue().accept(this));

            if (++processedKeys < dictLiteral.content().size()) {
                System.out.print(", ");
            }
        }

        System.out.print("})");
    }

    @Override
    public void visit(AndExpression andExpression) {
        visitBinOp("And", andExpression.left(), andExpression.right());
    }

    @Override
    public void visit(FloatLiteral floatLiteral) {
        print(String.format("Lit(%s)", floatLiteral.value()));
    }

    @Override
    public void visit(IntLiteral intLiteral) {
        print(String.format("Lit(%s)", intLiteral.value()));
    }

    @Override
    public void visit(StringLiteral stringLiteral) {
        print(String.format("Lit(\"%s\")", stringLiteral.value()));
    }

    @Override
    public void visit(MinusExpression minusExpression) {
        visitBinOp("Minus", minusExpression.left(), minusExpression.right());
    }

    @Override
    public void visit(ModuloExpression moduloExpression) {
        visitBinOp("Mod", moduloExpression.left(), moduloExpression.right());
    }

    @Override
    public void visit(MultiplyExpression multiplyExpression) {
        visitBinOp("Mul", multiplyExpression.left(), multiplyExpression.right());
    }

    @Override
    public void visit(NegationExpression negationExpression) {
        visitUnaryOp("Neg", negationExpression.expression());
    }

    @Override
    public void visit(UnaryMinusExpression unaryMinusExpression) {
        visitUnaryOp("UnaryMinus", unaryMinusExpression.expression());
    }

    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        print("DeclareVar(\n");
        withIndentation(() -> {
            print(String.format("name=%s\n", variableDeclaration.name()));
            print("value=");
            withoutIndentationForNextLine(() -> variableDeclaration.value().accept(this));
            print("\n");
        });
        print(")");
    }

    @Override
    public void visit(Null aNull) {
        print("Lit(null)");
    }

    @Override
    public void visit(OrExpression orExpression) {
        visitBinOp("Or", orExpression.left(), orExpression.right());
    }

    @Override
    public void visit(VariableValue variableValue) {
        print(String.format("Var(%s)", variableValue.varName()));
    }

    @Override
    public void visit(PlusExpression plusExpression) {
        visitBinOp("Plus", plusExpression.left(), plusExpression.right());
    }

    @Override
    public void visit(NullableExpression nullableExpression) {
        visitUnaryOp("Nullable", nullableExpression.expression());
    }

    @Override
    public void visit(LessThan lessThan) {
        visitBinOp("LessThan", lessThan.left(), lessThan.right());
    }
    
    @Override
    public void visit(LessThanOrEqual lessThanOrEqual) {
        visitBinOp("LessThanOrEq", lessThanOrEqual.left(), lessThanOrEqual.right());
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        visitBinOp("GreaterThan", greaterThan.left(), greaterThan.right());
    }

    @Override
    public void visit(GreaterThanOrEqual greaterThanOrEqual) {
        visitBinOp("GreaterThanOrEq", greaterThanOrEqual.left(), greaterThanOrEqual.right());
    }

    @Override
    public void visit(Equal equal) {
        visitBinOp("Eq", equal.left(), equal.right());
    }

    @Override
    public void visit(NotEqual notEqual) {
        visitBinOp("NotEq", notEqual.left(), notEqual.right());
    }

    private void visitUnaryOp(String opName, Expression expression) {
        print(String.format("%s(\n", opName));
        withIndentation(() -> expression.accept(this));
        print("\n");
        print(")");
    }

    private void visitBinOp(String opName, Expression left, Expression right) {
        print(String.format("%s(\n", opName));
        withIndentation(() -> {
            left.accept(this);
            print("\n");
            right.accept(this);
            print("\n");
        });
        print(")");
    }

    private void withIndentation(Runnable func) {
        indentation += INDENTATION_LEVEL;
        func.run();
        indentation -= INDENTATION_LEVEL;
    }

    private void withoutIndentationForNextLine(Runnable func) {
        nextPrintWithoutIndentation = true;
        func.run();
    }

    private void print(String value) {
        String result = nextPrintWithoutIndentation ? value : " ".repeat(indentation) + value ;
        nextPrintWithoutIndentation = false;
        System.out.print(result);
    }
}
