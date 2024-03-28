import lexer.FileCharacterProvider;
import lexer.StringCharacterProvider;

import java.io.IOException;

public class Interpreter {
    public static void main(String[] args) {
        try (var it = new FileCharacterProvider("a.txt")){
            String s = "";
            StringBuilder b = new StringBuilder();

            it.forEachRemaining(b::append);

            System.out.println(b.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
