import ast.AstPrinter;
import ast.Program;
import executor.DefaultProgramExecutor;
import executor.ProgramExecutor;
import executor.SemanticChecker;
import lexer.DefaultLexer;
import lexer.characterprovider.FileCharacterProvider;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import parser.DefaultParser;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Interpreter {
    private final ProgramExecutor executor;
    private final SemanticChecker semanticChecker;


    public Interpreter(ProgramExecutor executor, SemanticChecker semanticChecker) {
        this.executor = executor;
        this.semanticChecker = semanticChecker;
    }

    public static void main(String[] args) {
        Interpreter interpreter = new Interpreter(new DefaultProgramExecutor(), new SemanticChecker());
        interpreter.run(args);
    }

    private void run(String[] args) {
        Options options = buildCliOptions();

        CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        CommandLine cmd = null;

        try {
             cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            displayHelp(formatter, options);
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            displayHelp(formatter, options);
            System.exit(0);
        }

        String[] positionalArgs = cmd.getArgs();
        if (positionalArgs.length == 0) {
            displayHelp(formatter, options);
            System.exit(1);
        }

        String inputFilePath = positionalArgs[0];

        Program program = buildProgram(inputFilePath);

        if (cmd.hasOption("display-ast")) {
            new AstPrinter().visit(program);
            System.exit(0);
        }

        try {
            semanticChecker.visit(program);
        } catch (SemanticChecker.SemanticException e) {
            System.err.printf("Compilation error: %s%n", e.getMessage());
            System.exit(1);
        }

        try {
            executor.execute(program);
        } catch (RuntimeException e) {
            System.err.printf("Runtime error: %s%n", e.getMessage());
            System.exit(1);
        }
    }

    private Options buildCliOptions() {
        Options options = new Options();

        Option help = Option.builder()
                .option("h")
                .longOpt("help")
                .desc("Display help")
                .optionalArg(true)
                .numberOfArgs(0)
                .build();

        Option displayAst = Option.builder()
                .longOpt("display-ast")
                .desc("Display program abstract syntax tree")
                .optionalArg(true)
                .numberOfArgs(0)
                .build();

        options.addOption(help).addOption(displayAst);

        return options;
    }

    private Program buildProgram(String inputFilePath) {
        Program program = null;

        try (var characterProvider = new FileCharacterProvider(inputFilePath)) {
            var programParser = new parser.DefaultParser(new DefaultLexer(characterProvider));
            program = programParser.parseProgram();
        } catch (DefaultParser.SyntaxError | DefaultLexer.LexerException e) {
            System.err.println("Syntax error: " + e.getMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + inputFilePath);
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return program;
    }

    private void displayHelp(HelpFormatter formatter, Options options) {
        formatter.printHelp("java -jar <program.jar> [input file path]", options);
    }
}
