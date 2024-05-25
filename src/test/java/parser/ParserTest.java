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
import ast.expression.FloatLiteral;
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
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;
import ast.type.DictType;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.VoidType;
import lexer.Position;
import lexer.Token;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Collection;
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
import static lexer.TokenType.FOREACH_KEYWORD;
import static lexer.TokenType.GREATER_THAN_OPERATOR;
import static lexer.TokenType.GREATER_THAN_OR_EQUAL_OPERATOR;
import static lexer.TokenType.IDENTIFIER;
import static lexer.TokenType.IF_KEYWORD;
import static lexer.TokenType.INT_KEYWORD;
import static lexer.TokenType.INT_LITERAL;
import static lexer.TokenType.LEFT_CURLY_BRACKET;
import static lexer.TokenType.LEFT_ROUND_BRACKET;
import static lexer.TokenType.LEFT_SQUARE_BRACKET;
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
import static lexer.TokenType.RETURN_KEYWORD;
import static lexer.TokenType.RIGHT_CURLY_BRACKET;
import static lexer.TokenType.RIGHT_ROUND_BRACKET;
import static lexer.TokenType.RIGHT_SQUARE_BRACKET;
import static lexer.TokenType.SEMICOLON;
import static lexer.TokenType.STRING_KEYWORD;
import static lexer.TokenType.STRING_LITERAL;
import static lexer.TokenType.VOID_KEYWORD;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
    void should_throw_syntax_error_for_function_definition_without_argument_type() {
         /*
        given:

        int f1(a) {}
        */

        var tokens = List.of(getToken(INT_KEYWORD), getToken(IDENTIFIER, "f1"),
                getToken(LEFT_ROUND_BRACKET), getToken(IDENTIFIER, "a", new Position(1, 5)), getToken(RIGHT_ROUND_BRACKET),
                getToken(LEFT_CURLY_BRACKET), getToken(RIGHT_CURLY_BRACKET), Token.eof());

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessage("Expected right parentheses at position(line=1, column=5)");
    }

    @Test
    void should_not_allow_void_type_in_function_arguments() {
         /*
        given:

        int f(void a) {}
        */

        var tokens = List.of(getToken(INT_KEYWORD), getToken(IDENTIFIER, "f"), getToken(LEFT_ROUND_BRACKET),
                getToken(VOID_KEYWORD), getToken(IDENTIFIER, "a"), getToken(RIGHT_ROUND_BRACKET),
                getToken(LEFT_CURLY_BRACKET), getToken(RIGHT_CURLY_BRACKET), Token.eof());

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Expected right parentheses");
    }

    @Test
    void should_parse_global_variable_definition() {
        /*
        given:

        float x = 1.0f;
        */

        var tokens = List.of(getToken(FLOAT_KEYWORD), getToken(IDENTIFIER, "x"), getToken(ASSIGNMENT),
                getToken(FLOAT_LITERAL, 1.0f), getToken(SEMICOLON), Token.eof());

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions()).hasSize(0);
        assertThat(program.globalVariables()).hasSize(1);

        assertThat(program.globalVariables().get("x"))
                .extracting(v -> tuple(v.type(), v.name(), v.value()))
                .matches(t -> t.equals(tuple(new FloatType(), "x", new FloatLiteral(1.0f))));
    }

    @Test
    void should_not_allow_void_type_in_variable_declarations() {
        /*
        given:

        void x;
        */

        var tokens = List.of(getToken(VOID_KEYWORD), getToken(IDENTIFIER, "x"), getToken(SEMICOLON), Token.eof());

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Variable can't be of type void");
    }

    @Test
    void should_throw_syntax_error_when_global_variable_definition_with_missing_expression() {
        /*
        given:

        float x =;
        */

        var tokens = List.of(getToken(FLOAT_KEYWORD), getToken(IDENTIFIER, "x"),
                getToken(ASSIGNMENT), getToken(SEMICOLON), Token.eof());

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessage("Missing expression");
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
                                                new FunctionCall("fun2", List.of(new IntLiteral(1)), new Position(0, 0)),
                                                new StringLiteral("xyz")
                                        ), new Position(0, 0)),
                                        new IntLiteral(9)
                                ),
                                new VariableValue("x")
                        ),
                        new Position(0, 0)
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
                        ),
                        new Position(0, 0)
                ));
    }

    @Test
    void should_throw_syntax_error_for_expression_with_missing_operand() {
  /*
        given:

        return 1 +;
        */

        var body = List.of(getToken(INT_LITERAL, 1), getToken(PLUS_OPERATOR));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(
                        INT_KEYWORD, "f", List.of(),
                        TokenFactory.returnStatement(body)
                )
        ));

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Missing right side of operator");
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
                        ),
                        new Position(0, 0)
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
                        ),
                        new Position(0, 0)
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
                .isEqualTo(new ReturnStatement(
                        new NullableExpression(new GreaterThanOrEqual(
                                new IntLiteral(1),
                                Null.getInstance()
                        )),
                        new Position(0, 0)
                ));
    }

    @Test
    void should_throw_syntax_error_for_return_statement_without_trailing_semicolon() {
         /*
        given:

        return 1.0;
        */

        var body = List.of(getToken(RETURN_KEYWORD), getToken(FLOAT_LITERAL, 1.0f));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(INT_KEYWORD, "f", List.of(), body)
        ));

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Missing semicolon");
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
    void should_throw_syntax_error_for_while_statement_without_condition() {
         /*
        given:

        while()
        */

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(VOID_KEYWORD, "f", List.of(), TokenFactory.whileStatement(List.of(), List.of()))
        ));

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Expected condition in while statement");
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
    void should_throw_syntax_error_for_missing_left_round_bracket_in_foreach_statement() {
  /*
        given:

        foreach int x : y) {}
        */

        var body = List.of(getToken(FOREACH_KEYWORD), getToken(INT_KEYWORD), getToken(IDENTIFIER, "x"), getToken(COLON),
                getToken(IDENTIFIER, "y"), getToken(RIGHT_ROUND_BRACKET), getToken(LEFT_CURLY_BRACKET), getToken(RIGHT_CURLY_BRACKET));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(
                        VOID_KEYWORD, "f", List.of(),
                        body
                )
        ));

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Expected left round bracket");
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
        var ifBody = List.of(getToken(IDENTIFIER, "a", new Position(2, 4)), getToken(LEFT_ROUND_BRACKET), getToken(RIGHT_ROUND_BRACKET), getToken(SEMICOLON));
        var elseBody = List.of(getToken(IDENTIFIER, "b", new Position(4, 4)), getToken(LEFT_ROUND_BRACKET), getToken(RIGHT_ROUND_BRACKET), getToken(SEMICOLON));

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
                        List.of(new FunctionCall("a", Collections.emptyList(), new Position(2, 4))),
                        List.of(new FunctionCall("b", Collections.emptyList(), new Position(4, 4)))
                )));
    }

    @Test
    void should_throw_syntax_error_for_statement_block_with_missing_curly_bracket() {
  /*
        given:

        if(1) }
        */

        var body = List.of(getToken(IF_KEYWORD), getToken(LEFT_ROUND_BRACKET), getToken(INT_LITERAL, 1),
                getToken(RIGHT_ROUND_BRACKET), getToken(RIGHT_CURLY_BRACKET, null, new Position(3, 7)));

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(
                        VOID_KEYWORD, "f", List.of(),
                        body
                )
        ));

        // then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(tokens))
                .withMessageContaining("Expected left curly bracket at position(line=3, column=7)");
    }

    @Test
    void should_parse_variable_declaration() {
        /*
        given:
        dict[string, int] some_dict = {"a": 1};
        int x1 = null;
        float x2;
        */

        List<Token> body = Stream.of(
                List.of(getToken(DICT_KEYWORD), getToken(LEFT_SQUARE_BRACKET), getToken(STRING_KEYWORD),
                        getToken(COMMA), getToken(INT_KEYWORD), getToken(RIGHT_SQUARE_BRACKET)),
                List.of(getToken(IDENTIFIER, "some_dict"), getToken(ASSIGNMENT), getToken(LEFT_CURLY_BRACKET),
                        getToken(STRING_LITERAL, "a"), getToken(COLON), getToken(INT_LITERAL, 1),
                        getToken(RIGHT_CURLY_BRACKET), getToken(SEMICOLON)),
                List.of(getToken(INT_KEYWORD), getToken(IDENTIFIER, "x1"), getToken(ASSIGNMENT),
                        getToken(NULL_KEYWORD), getToken(SEMICOLON)),
                List.of(getToken(FLOAT_KEYWORD), getToken(IDENTIFIER, "x2"), getToken(SEMICOLON))
        ).flatMap(Collection::stream).toList();

        var tokens = TokenFactory.program(List.of(
                TokenFactory.function(VOID_KEYWORD, "f", List.of(), body)
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions().get("f").statementBlock())
                .element(0)
                .asInstanceOf(InstanceOfAssertFactories.type(VariableDeclaration.class))
                .matches(s -> s.name().equals("some_dict"))
                .matches(s -> s.type().equals(new DictType(new StringType(), new IntType())))
                .matches(s -> s.value().equals(new DictLiteral(Map.of(new StringLiteral("a"), new IntLiteral(1)))));

        assertThat(program.functions().get("f").statementBlock())
                .element(1)
                .asInstanceOf(InstanceOfAssertFactories.type(VariableDeclaration.class))
                .matches(s -> s.name().equals("x1"))
                .matches(s -> s.type().equals(new IntType()))
                .matches(s -> s.value().equals(Null.getInstance()));

        assertThat(program.functions().get("f").statementBlock())
                .element(2)
                .asInstanceOf(InstanceOfAssertFactories.type(VariableDeclaration.class))
                .matches(s -> s.name().equals("x2"))
                .matches(s -> s.type().equals(new FloatType()))
                .matches(s -> s.value().equals(Null.getInstance()));
    }


    private Program parseProgram(List<Token> tokens) {
        return new DefaultParser(new MockLexer(tokens)).parseProgram();
    }
}
