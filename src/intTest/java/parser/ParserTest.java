package parser;

import ast.FunctionCall;
import ast.Parameter;
import ast.Program;
import ast.StatementBlock;
import ast.expression.CastedExpression;
import ast.expression.FloatLiteral;
import ast.expression.IntLiteral;
import ast.expression.LessThan;
import ast.expression.MinusExpression;
import ast.expression.MultiplyExpression;
import ast.expression.Null;
import ast.expression.PlusExpression;
import ast.expression.VariableValue;
import ast.statement.ReturnStatement;
import ast.statement.VariableAssignment;
import ast.statement.VariableDeclaration;
import ast.statement.WhileStatement;
import ast.type.IntType;
import ast.type.VoidType;
import lexer.DefaultLexer;
import lexer.Position;
import lexer.characterprovider.StringCharacterProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

public class ParserTest {
    @Test
    void should_parse_program_ast() {
        // given
        String input = """        
        int sq(int x) {
            return x * x;
        }
        
        void main() {
            int x = 1.0 as int;
            while (x < 5 - 1) {
                print(sq(x));
                x = x + 1;
            }
        }
        
        int globalX;
        """;

        //when
        Program program = parseProgram(input);

        // then
        assertThat(program.globalVariables()).hasSize(1);
        assertThat(program.globalVariables().get("globalX"))
                .matches(v -> v.equals(new VariableDeclaration(
                        new IntType(),
                        "globalX",
                        Null.getInstance(),
                        new Position(13, 1)
                )));

        assertThat(program.functions()).hasSize(2);

        assertThat(program.functions().get("sq"))
                .extracting(f -> tuple(f.name(), f.returnType(), f.position(), f.parameters()))
                .matches(t -> t.equals(tuple(
                        "sq",
                        new IntType(),
                        new Position(1, 1),
                        List.of(new Parameter(new IntType(), "x"))
                )));

        assertThat(program.functions().get("sq").statementBlock().statements())
                .hasSize(1)
                .first()
                .matches(s -> s.equals(new ReturnStatement(
                        new MultiplyExpression(new VariableValue("x"), new VariableValue("x")),
                        new Position(2, 5)
                )));

        assertThat(program.functions().get("main"))
                .extracting(f -> tuple(f.name(), f.returnType(), f.position(), f.parameters()))
                .matches(t -> t.equals(tuple(
                        "main",
                        new VoidType(),
                        new Position(5, 1),
                        List.of()
                )));

        assertThat(program.functions().get("main").statementBlock().statements()).hasSize(2);

        assertThat(program.functions().get("main").statementBlock().statements())
                .element(0)
                .matches(s -> s.equals(new VariableDeclaration(
                        new IntType(),
                        "x",
                        new CastedExpression(new FloatLiteral(1.0f), new IntType()),
                        new Position(6, 5)
                )));

        assertThat(program.functions().get("main").statementBlock().statements())
                .element(1)
                .matches(s -> s.equals(new WhileStatement(
                        new LessThan(new VariableValue("x"), new MinusExpression(new IntLiteral(5), new IntLiteral(1))),
                        new StatementBlock(List.of(
                                new FunctionCall(
                                        "print",
                                        List.of(new FunctionCall(
                                                "sq",
                                                List.of(new VariableValue("x")),
                                                new Position(8, 15)
                                        )),
                                        new Position(8, 9)
                                ),
                                new VariableAssignment(
                                        "x",
                                        new PlusExpression(new VariableValue("x"), new IntLiteral(1)),
                                        new Position(9, 9)
                                )
                        )),
                        new Position(7, 5)
                )));

    }

    @Test
    void should_throw_syntax_error_if_program_cant_be_parsed() {
        // given
        String input = """
                void main() {
                    do_smth(;
                }
                """;

        //then
        assertThatExceptionOfType(DefaultParser.SyntaxError.class)
                .isThrownBy(() -> parseProgram(input))
                .withMessage("Missing right round bracket at position(line=2, column=13)");
    }

        private Program parseProgram(String program) {
        return new DefaultParser(new DefaultLexer(new StringCharacterProvider(program))).parseProgram();
    }
}
