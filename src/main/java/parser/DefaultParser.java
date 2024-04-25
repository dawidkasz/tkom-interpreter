package parser;

import lexer.Lexer;
import lexer.Token;
import parser.statement.FunctionDefinition;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultParser implements Parser {
    private final Lexer lexer;

    private Token token;

    public DefaultParser(Lexer lexer) {
        this.lexer = lexer;
        consumeToken();
    }

    @Override
    public Program parseProgram() {
        return Stream.generate(this::parseFunDef)
                .takeWhile(Optional::isPresent)
                .map(Optional::orElseThrow)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(FunctionDefinition::getName, FunctionDefinition::getDef),
                        Program::new
                ));
    }

    private Optional<FunctionDefinition> parseFunDef() {
        return Optional.of(new FunctionDefinition("",""));
    }

    private void consumeToken() {
        this.token = lexer.nextToken();
    }
}
