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

    public String getFunctionName() {
        return functionName;
    }

    public Variable findVar(String varName) {
        return findVarScope(varName).orElseThrow().get(varName);
    }

    public void declareVar(Variable newVar) {
        if (blockScopes.isEmpty()) {
            blockScopes.push(new Scope());
        }

        Scope currentScope = blockScopes.peek();

        assert currentScope != null;

        if (currentScope.contains(newVar.getName())) {
            throw new RuntimeException("Variable already defined in current scope: " + newVar);
        }

        currentScope.declareVar(newVar);
    }

    public void assignVar(String varName, Object value) {
        Scope scope = findVarScope(varName).orElseThrow();
        scope.assignVar(varName, value);
    }


    private Optional<Scope> findVarScope(String varName) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        blockScopes.descendingIterator(), Spliterator.ORDERED), false)
                .filter(scope -> scope.contains(varName))
                .findFirst();
    }
}
