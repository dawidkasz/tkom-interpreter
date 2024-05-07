import ast.AstPrinter;
import lexer.DefaultLexer;
import lexer.characterprovider.StringCharacterProvider;
import parser.DefaultParser;

public class Interpreter {
    public static void main(String[] args) {
        var input1 = """
        int abc = 100;

        int sq(int a, int b) {
            return a * a + b * b;
        }
        
        void main() {
            int x = 1;
            int y = 2;
            
            while (x + y <= abc) {
                print(sq(x, y) as string);
                x = x + 2;
                y = y * y;
            }
        }
        """;

        var input2 = """
        void main() {
            dict[string, float] someDict = {"a": 1.0, "b": 2.0};
        
            foreach(string x : someDict) {
                if (someDict[x] < 1.5) {
                    print("less");
                } else {
                    print("more");
                }
            }
        }
        """;

        var parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input1)));

        var program = parser.parseProgram();

        var printer = new AstPrinter();
        printer.visit(program);
    }
}
