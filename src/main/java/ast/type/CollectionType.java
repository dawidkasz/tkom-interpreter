package ast.type;

import java.util.List;

public interface CollectionType extends Type {
    List<SimpleType> paramTypes();
}
