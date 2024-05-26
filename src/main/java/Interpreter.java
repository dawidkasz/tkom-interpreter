import ast.AstPrinter;
import ast.Program;
import executor.Executor;
import lexer.DefaultLexer;
import lexer.characterprovider.FileCharacterProvider;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Interpreter {
    public static void main(String[] args) {
        Interpreter interpreter = new Interpreter();
        interpreter.run(args);
    }

    private void run(String[] args) {
        Options options = buildCliOptions();

        CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

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

            Executor executor = new Executor();
            executor.visit(program);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            displayHelp(formatter, options);
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
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + inputFilePath);
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
