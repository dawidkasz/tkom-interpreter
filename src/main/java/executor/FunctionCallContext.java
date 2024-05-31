package executor;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
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
        return findVarScope(varName).orElseThrow().get(varName);
    }

    public void setVar(String varName, Object value) {
        if (blockScopes.isEmpty()) {
            blockScopes.push(new Scope());
        }

        var scope = blockScopes.peek();

        assert scope != null;
        scope.assign(varName, value);
    }

    public void updateVar(String varName, Object value) {
        Scope scope = findVarScope(varName).orElseThrow();
        scope.assign(varName, value);
    }


    private Optional<Scope> findVarScope(String varName) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        blockScopes.descendingIterator(), Spliterator.ORDERED), false)
                .filter(scope -> scope.contains(varName))
                .findFirst();
    }
}
