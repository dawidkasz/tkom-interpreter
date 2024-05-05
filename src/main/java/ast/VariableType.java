package ast;

public enum VariableType {
    INT,
    FLOAT,
    STRING,
    DICT;

    public boolean isSimpleType() {
        return this != VariableType.DICT;
    }

    public boolean isCollectionType() {
        return !isSimpleType();
    }
}
