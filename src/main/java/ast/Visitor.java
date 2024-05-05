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

public interface Visitor {
    void visit(Program program);
    void visit(FunctionDefinition functionDefinition);
    void visit(WhileStatement whileStatement);
    void visit(ReturnStatement returnStatement);
    void visit(FunctionCall functionCall);
    void visit(DivideExpression divideExpression);
    void visit(CastedExpression castedExpression);
    void visit(DictKeyValue dictKeyValue);
    void visit(DictLiteral dictLiteral);
    void visit(AndExpression andExpression);
    void visit(FloatLiteral floatLiteral);
    void visit(IntLiteral intLiteral);
    void visit(LessThanExpression lessThanExpression);
    void visit(MinusExpression minusExpression);
    void visit(ModuloExpression moduloExpression);
    void visit(MultiplyExpression multiplyExpression);
    void visit(NegatedExpression negatedExpression);
    void visit(Null aNull);
    void visit(OrExpression orExpression);
}
