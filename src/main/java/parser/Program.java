package parser;

import java.util.HashMap;
import java.util.Map;

public class Program {
    private final Map<String, String> functions;

    public Program(Map<String, String> functions) {
        this.functions = new HashMap<>(functions);
    }
}
