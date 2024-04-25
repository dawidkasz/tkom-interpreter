package parser.statement;

public class FunctionDefinition {
    private final String name;
    private final String def;

    public FunctionDefinition(String name, String def) {
        this.name = name;
        this.def = def;
    }

    public String getName() {
        return name;
    }

    public String getDef() {
        return def;
    }
}
