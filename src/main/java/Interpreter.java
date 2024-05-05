import lexer.DefaultLexer;
import lexer.characterprovider.StringCharacterProvider;
import parser.DefaultParser;

public class Interpreter {
    public static void main(String[] args) {
        String input = """
        void f() {
            return 1
        }
        
        int main(int a, int b, float c, dict[int, int] d) {
            return 1 || 2 || 3
        }
        
         int add(int a, int b) {}
         
        int get() {
            while (3 || 2 && 2) {
               return 5
            }
            return 1
        }
        
        int get2() {
            return {"a": 2, 2: 1, 3: "c", "a": 5.5}
        }
        
        int get3() {
            return 1 + 2 - 3 + 4 / 2 < 5 % 4 + 1 * 7 * 7 % 3 / 2 && 1 < 2
        }
        
        int get4() {
            return (2 < 3) ? || "3" as int < 4 || !(6 < 5)
        }
        
        
        """;

        var parser = new DefaultParser(new DefaultLexer(new StringCharacterProvider(input)));

        var program = parser.parseProgram();
        System.out.append(program.toString());
    }
}
