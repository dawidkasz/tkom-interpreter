import ast.AstPrinter;
import lexer.DefaultLexer;
import lexer.characterprovider.StringCharacterProvider;
import parser.DefaultParser;

public class Interpreter {
    public static void main(String[] args) {
        String input = """
        void f() {
            return 1;
        }
        
        int main(int a, int b, float c, dict[int, int] d) {
            return 1 || 2 || 3;
        }
        
         int add(int a, int b) {}
         
        int get() {
            while (3 || 2 && 2) {
               return 5;
            }
            return null;
        }
        
        int get2() {
            return {"a": 2, 2: 1, 3: "c", "a": 5.5};
        }
        
        int get3() {
            return 1 + 2 - 3 + 4 / 2 < 5 % 4 + 1 * 7 * 7 % 3 / 2 && 1 < 2;
        }
        
        int get4() {
            return (2 < 3) ? || "3" as int < 4 || !(6 < 5);
        }
        
        int get5() {
            return f1(3, 4, xyz() + 5) || d1[3] == "32" && d1[x(2, 4)] == f5(1,1)[4];
        }
        """;

        String input2 = """
        int fun1(int x, float y) {
            while (3 || 4) {
                return 2;
            }
        
            return 2 / 2 || ((2+2) * (3-3+3)) as string;
        }
        
        int fun2() {
            return (x+2)?;
        }
        
        int fun3() {
            return f(3, 4, 5+5-3) + y;
        }
        
        int fun4() {
            return {f(): 4 + 2 - 1, 2: 5}[2];
        }
        """;

        var parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input2)));

        var program = parser.parseProgram();

        var printer = new AstPrinter();
        printer.visit(program);
    }
}
