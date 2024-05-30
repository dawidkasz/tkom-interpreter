package executor;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

final class FunctionCallContext {
    private final String functionName;
    private final Deque<Scope> blockScopes = new LinkedList<>();

    FunctionCallContext(String functionName) {
        this.functionName = functionName;
    }

    FunctionCallContext(String functionName, Scope initialScope) {
        this.functionName = functionName;
        this.blockScopes.add(initialScope);
    }

    public Object findVar(String varName) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                blockScopes.descendingIterator(), Spliterator.ORDERED), false)
                .filter(scope -> scope.contains(varName))
                .findFirst()
                .orElseThrow()
                .get(varName);
    }
}
