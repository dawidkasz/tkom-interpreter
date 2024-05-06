package lexer;

import ast.FunctionDefinition;
import ast.Parameter;
import ast.Program;
import ast.type.DictType;
import ast.type.FloatType;
import ast.type.IntType;
import ast.type.StringType;
import ast.type.VoidType;
import org.junit.jupiter.api.Test;
import parser.DefaultParser;

import java.util.List;

import static lexer.TokenType.DICT_KEYWORD;
import static lexer.TokenType.FLOAT_KEYWORD;
import static lexer.TokenType.INT_KEYWORD;
import static lexer.TokenType.STRING_KEYWORD;
import static lexer.TokenType.VOID_KEYWORD;
import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {
    @Test
    void should_parse_function_definitions() {
        /*
        given:

        int f1(float a, dict[int, string] b, int c) {}
        void f2() {}
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
            TokenFactory.function(VOID_KEYWORD, "f2", List.of(), List.of())
        ));

        // when
        var program = parseProgram(tokens);

        // then
        assertThat(program.functions()).hasSize(2);

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
    }

    private Program parseProgram(List<Token> tokens) {
        var lexer = new Lexer() {
            private final List<Token> tk = tokens;
            private int current = 0;
            @Override
            public Token nextToken() {
//                if (current == tokens.size()) {
//                    return
//                }
                return tk.get(current++);
            }
        };

        return new DefaultParser(lexer).parseProgram();
    }
}
