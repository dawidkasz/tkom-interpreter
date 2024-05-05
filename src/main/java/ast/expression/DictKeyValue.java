package ast.expression;

public class DictKeyValue implements Expression {
    private final Expression dict;
    private final Expression key;

    public DictKeyValue(Expression dict, Expression key) {
        this.dict = dict;
        this.key = key;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", dict, key);
    }
}
