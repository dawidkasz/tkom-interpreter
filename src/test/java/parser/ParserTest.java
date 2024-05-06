package parser;

import ast.FunctionCall;
import ast.FunctionDefinition;
import ast.Parameter;
import ast.Program;
import ast.expression.AndExpression;
import ast.expression.CastedExpression;
import ast.expression.DictLiteral;
import ast.expression.DivideExpression;
import ast.expression.Equal;
import ast.expression.GreaterThan;
import ast.expression.GreaterThanOrEqual;
import ast.expression.IntLiteral;
import ast.expression.LessThan;
import ast.expression.LessThanOrEqual;
import ast.expression.MinusExpression;
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
import ast.statement.ForeachStatement;
import ast.statement.IfStatement;
import ast.statement.ReturnStatement;
import ast.statement.WhileStatement;
import ast.type.DictType;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.VoidType;
import lexer.Token;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static lexer.TokenType.AND_OPERATOR;
import static lexer.TokenType.ASSIGNMENT;
import static lexer.TokenType.AS_KEYWORD;
import static lexer.TokenType.COLON;
import static lexer.TokenType.COMMA;
import static lexer.TokenType.DICT_KEYWORD;
import static lexer.TokenType.DIVISION_OPERATOR;
import static lexer.TokenType.EQUAL_OPERATOR;
import static lexer.TokenType.FLOAT_KEYWORD;
import static lexer.TokenType.FLOAT_LITERAL;
import static lexer.TokenType.GREATER_THAN_OPERATOR;
import static lexer.TokenType.GREATER_THAN_OR_EQUAL_OPERATOR;
import static lexer.TokenType.IDENTIFIER;
import static lexer.TokenType.INT_KEYWORD;
import static lexer.TokenType.INT_LITERAL;
import static lexer.TokenType.LEFT_CURLY_BRACKET;
import static lexer.TokenType.LEFT_ROUND_BRACKET;
import static lexer.TokenType.LESS_THAN_OPERATOR;
import static lexer.TokenType.LESS_THAN_OR_EQUAL_OPERATOR;
import static lexer.TokenType.MINUS_OPERATOR;
import static lexer.TokenType.MULTIPLICATION_OPERATOR;
import static lexer.TokenType.NEGATION_OPERATOR;
import static lexer.TokenType.NOT_EQUAL_OPERATOR;
import static lexer.TokenType.NULLABLE_OPERATOR;
import static lexer.TokenType.NULL_KEYWORD;
import static lexer.TokenType.OR_OPERATOR;
import static lexer.TokenType.PLUS_OPERATOR;
import static lexer.TokenType.RIGHT_CURLY_BRACKET;
import static lexer.TokenType.RIGHT_ROUND_BRACKET;
import static lexer.TokenType.SEMICOLON;
import static lexer.TokenType.STRING_KEYWORD;
import static lexer.TokenType.STRING_LITERAL;
import static lexer.TokenType.VOID_KEYWORD;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static parser.TokenFactory.getToken;
import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {
    @Test
    void should_parse_function_definitions() {
        /*
        given:

        int f1(float a, dict[int, string] b, int c) {}
        void f2() {}
        dict[float, float] f3() {}
        */

        var tokens = TokenFactory.program(List.of(
            TokenFactory.function(
                    INT_KEYWORD, "f1",
                    TokenFactory.parameters(List.of(
                            TokenFactory.parameter(FLOAT_KEYWORD, "a"),
                            TokenFactory.parameter(DICT_KEYWORD, INT_KEYWORD, STRING_KEYWORD, "b"),
                            TokenFactory.parameter(INT_KEYWORD, "c")
                    )),
                    List.of()
            ),
            TokenFactory.function(VOID_KEYWORD, "f2", List.of(), List.of()),
            TokenFactory.function(TokenFactory.dict(FLOAT_KEYWORD, FLOAT_KEYWORD), "f3", List.of(), List.of())
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions()).hasSize(3);

        assertThat(program.functions().get("f1"))
                .matches(f -> f.name().equals("f1"))
                .matches(f -> f.returnType().equals(new IntType()))
                .extracting(FunctionDefinition::parameters)
                .isEqualTo(List.of(
                        new Parameter(new FloatType(), "a"),
                        new Parameter(new DictType(new IntType(), new StringType()), "b"),
                        new Parameter(new IntType(), "c")
                ));

        assertThat(program.functions().get("f2"))
                .matches(f -> f.name().equals("f2"))
                .matches(f -> f.returnType().equals(new VoidType()))
                .extracting(FunctionDefinition::parameters)
                .matches(List::isEmpty);

        assertThat(program.functions().get("f3"))
                .matches(f -> f.name().equals("f3"))
                .matches(f -> f.returnType().equals(new DictType(new FloatType(), new FloatType())))
                .extracting(FunctionDefinition::parameters)
                .matches(List::isEmpty);
    }

    @Test
    void should_parse_return_additive_expression_statement_with_nested_function_calls() {
        /*
        given:

        return fun(fun2(1), "xyz") + 9 - x;
        */

        var expression = List.of(getToken(IDENTIFIER, "fun"), getToken(LEFT_ROUND_BRACKET),
                getToken(IDENTIFIER, "fun2"), getToken(LEFT_ROUND_BRACKET), getToken(INT_LITERAL, 1), getToken(RIGHT_ROUND_BRACKET),
                getToken(COMMA), getToken(STRING_LITERAL, "xyz"), getToken(RIGHT_ROUND_BRACKET), getToken(PLUS_OPERATOR),
                getToken(INT_LITERAL, 9), getToken(MINUS_OPERATOR), getToken(IDENTIFIER, "x"));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(INT_KEYWORD, "f", List.of(), TokenFactory.returnStatement(expression))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .hasSize(1)
                .first()
                .isEqualTo(new ReturnStatement(
                        new MinusExpression(
                                new PlusExpression(
                                        new FunctionCall("fun", List.of(
                                                new FunctionCall("fun2", List.of(new IntLiteral(1))),
                                                new StringLiteral("xyz")
                                        )),
                                        new IntLiteral(9)
                                ),
                                new VariableValue("x")
                        )
                ));
    }

    @Test
    void should_parse_return_expression_with_different_operator_priorities() {
        /*
        given:

        return 1 + 2 / ((3 - 4) * 5) - 6;
        */

        var expression = List.of(getToken(INT_LITERAL, 1), getToken(PLUS_OPERATOR), getToken(INT_LITERAL, 2),
                getToken(DIVISION_OPERATOR), getToken(LEFT_ROUND_BRACKET), getToken(LEFT_ROUND_BRACKET),
                getToken(INT_LITERAL, 3), getToken(MINUS_OPERATOR), getToken(INT_LITERAL, 4), getToken(RIGHT_ROUND_BRACKET),
                getToken(MULTIPLICATION_OPERATOR), getToken(INT_LITERAL, 5), getToken(RIGHT_ROUND_BRACKET),
                getToken(MINUS_OPERATOR), getToken(INT_LITERAL, 6));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(FLOAT_KEYWORD, "f", List.of(), TokenFactory.returnStatement(expression))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .hasSize(1)
                .first()
                .isEqualTo(new ReturnStatement(
                        new MinusExpression(
                                new PlusExpression(
                                        new IntLiteral(1),
                                        new DivideExpression(
                                                new IntLiteral(2),
                                                new MultiplyExpression(
                                                        new MinusExpression(
                                                                new IntLiteral(3),
                                                                new IntLiteral(4)
                                                        ),
                                                        new IntLiteral(5)
                                                )
                                        )
                                ),
                                new IntLiteral(6)
                        )
                ));
    }

    @Test
    void should_parse_return_conditional_expression() {
        /*
        given:

        return !(x == 1) || y != 9 && x < 10;
        */

        var expression = List.of(getToken(NEGATION_OPERATOR), getToken(LEFT_ROUND_BRACKET), getToken(IDENTIFIER, "x"),
                getToken(EQUAL_OPERATOR), getToken(INT_LITERAL, 1), getToken(RIGHT_ROUND_BRACKET),
                getToken(OR_OPERATOR), getToken(IDENTIFIER, "y"), getToken(NOT_EQUAL_OPERATOR), getToken(INT_LITERAL, 9),
                getToken(AND_OPERATOR), getToken(IDENTIFIER, "x"), getToken(LESS_THAN_OPERATOR), getToken(INT_LITERAL, 10));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(INT_KEYWORD, "f", List.of(), TokenFactory.returnStatement(expression))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .hasSize(1)
                .first()
                .isEqualTo(new ReturnStatement(
                        new OrExpression(
                                new NegationExpression(new Equal(new VariableValue("x"), new IntLiteral(1))),
                                new AndExpression(
                                        new NotEqual(new VariableValue("y"), new IntLiteral(9)),
                                        new LessThan(new VariableValue("x"), new IntLiteral(10))
                                )
                        )
                ));
    }

    @Test
    void should_parse_return_unary_minus_and_cast_expressions() {
        /*
        given:

        return -("1" as int);
        */

        var expression = List.of(getToken(MINUS_OPERATOR), getToken(LEFT_ROUND_BRACKET), getToken(STRING_LITERAL, "1"),
                getToken(AS_KEYWORD), getToken(INT_KEYWORD), getToken(RIGHT_ROUND_BRACKET));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(INT_KEYWORD, "f", List.of(), TokenFactory.returnStatement(expression))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .hasSize(1)
                .first()
                .isEqualTo(new ReturnStatement(
                        new UnaryMinusExpression(
                                new CastedExpression(new StringLiteral("1"), new IntType())
                        )
                ));
    }

    @Test
    void should_parse_return_nullable_expression() {
        /*
        given:

        return (1 >= null)?;
        */

        var expression = List.of(getToken(LEFT_ROUND_BRACKET), getToken(INT_LITERAL, 1), getToken(GREATER_THAN_OR_EQUAL_OPERATOR),
                getToken(NULL_KEYWORD), getToken(RIGHT_ROUND_BRACKET), getToken(NULLABLE_OPERATOR));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(INT_KEYWORD, "f", List.of(), TokenFactory.returnStatement(expression))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .hasSize(1)
                .first()
                .isEqualTo(new ReturnStatement(new NullableExpression(new GreaterThanOrEqual(
                        new IntLiteral(1),
                        Null.getInstance()
                ))));
    }

    @Test
    void should_parse_while_statement() {
        /*
        given:

        while(x <= 5) {
            some_func();
            x = x + 1;
            while(y) {}
        }
        */

        var condition = List.of(getToken(IDENTIFIER, "x"), getToken(LESS_THAN_OR_EQUAL_OPERATOR), getToken(INT_LITERAL, 5));

        var body = List.of(getToken(IDENTIFIER, "some_func"), getToken(LEFT_ROUND_BRACKET), getToken(RIGHT_ROUND_BRACKET),
                getToken(SEMICOLON), getToken(IDENTIFIER, "x"), getToken(ASSIGNMENT), getToken(IDENTIFIER, "x"),
                getToken(PLUS_OPERATOR), getToken(INT_LITERAL, 1), getToken(SEMICOLON));

        body = Stream.concat(body.stream(), TokenFactory.whileStatement(List.of(getToken(IDENTIFIER, "y")), List.of()).stream()).toList();

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(VOID_KEYWORD, "f", List.of(), TokenFactory.whileStatement(condition, body))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(WhileStatement.class))
                .matches(s -> s.condition().equals(new LessThanOrEqual(new VariableValue("x"), new IntLiteral(5))))
                .extracting(WhileStatement::statementBlock)
                .matches(block -> block.size() == 3);
    }

    @Test
    void should_parse_foreach_statement_with_dict_literal() {
        /*
        given:

        foreach(string x : {"x": a, "y": 2}) {
            some_f();
        }
        */

        var iterable = List.of(getToken(LEFT_CURLY_BRACKET), getToken(STRING_LITERAL, "x"), getToken(COLON),
                getToken(IDENTIFIER, "a"), getToken(COMMA), getToken(STRING_LITERAL, "y"), getToken(COLON),
                getToken(INT_LITERAL, 2), getToken(RIGHT_CURLY_BRACKET));

        var body = List.of(getToken(IDENTIFIER, "some_f"), getToken(LEFT_ROUND_BRACKET),
                getToken(RIGHT_ROUND_BRACKET), getToken(SEMICOLON));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(
                        VOID_KEYWORD, "f", List.of(),
                        TokenFactory.foreachStatement(STRING_KEYWORD, "x", iterable, body)
                )
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(ForeachStatement.class))
                .matches(s -> s.varType().equals(new StringType()))
                .matches(s -> s.varName().equals("x"))
                .matches(s -> s.iterable().equals(new DictLiteral(Map.of(
                        new StringLiteral("x"), new VariableValue("a"),
                        new StringLiteral("y"), new IntLiteral(2)
                ))))
                .extracting(ForeachStatement::statementBlock)
                .matches(block -> block.size() == 1);
    }

    @Test
    void should_parse_if_else_statement() {
        /*
        given:

        if (x == "abc") {
            a();
        } else {
            b();
        }
        */

        var condition = List.of(getToken(IDENTIFIER, "x"), getToken(GREATER_THAN_OPERATOR), getToken(STRING_LITERAL, "abc"));
        var ifBody = List.of(getToken(IDENTIFIER, "a"), getToken(LEFT_ROUND_BRACKET), getToken(RIGHT_ROUND_BRACKET), getToken(SEMICOLON));
        var elseBody = List.of(getToken(IDENTIFIER, "b"), getToken(LEFT_ROUND_BRACKET), getToken(RIGHT_ROUND_BRACKET), getToken(SEMICOLON));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(VOID_KEYWORD, "f", List.of(), TokenFactory.ifElseStatement(condition, ifBody, elseBody))
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(IfStatement.class))
                .matches(s -> s.condition().equals(new GreaterThan(new VariableValue("x"), new StringLiteral("abc"))))
                .extracting(s -> tuple(s.ifBlock(), s.elseBlock()))
                .matches(t -> t.equals(tuple(
                        List.of(new FunctionCall("a", Collections.emptyList())),
                        List.of(new FunctionCall("b", Collections.emptyList()))
                )));
    }


    private Program parseProgram(List<Token> tokens) {
        return new DefaultParser(new MockLexer(tokens)).parseProgram();
    }
}
