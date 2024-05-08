package ast.type;

import java.util.List;

public record DictType(SimpleType keyType, SimpleType valueType) implements CollectionType {
    @Override
    public List<SimpleType> paramTypes() {
        return List.of(keyType, valueType);
    }

    @Override
    public String toString() {
        return String.format("dict(key=%s value=%s)", keyType, valueType);
    }
}
