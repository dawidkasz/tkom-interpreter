package executor;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

final class FunctionCallContext {
    private final String functionName;
    private final Deque<Scope> blockScopes = new LinkedList<>();

    FunctionCallContext(String functionName) {
        this.functionName = functionName;
    }

    FunctionCallContext(String functionName, Scope argumentScope, Scope globalScope) {
        this.functionName = functionName;
        this.blockScopes.add(globalScope);
        this.blockScopes.add(argumentScope);
    }

    public String getFunctionName() {
        return functionName;
    }

    public void addScope() {
        blockScopes.push(new Scope());
    }

    public void removeScope() {
        blockScopes.pop();
    }

    public Optional<Variable> findVar(String varName) {
        Optional<Scope> scope = findVarScope(varName);

        return scope.isPresent() ? scope.get().get(varName) : Optional.empty();
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
        return blockScopes.stream().filter(scope -> scope.contains(varName)).findFirst();
    }
}
