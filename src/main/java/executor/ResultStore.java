package executor;

final class ResultStore<T> {
    private T lastResult = null;

    public void store(T value) {
        lastResult = value;
    }

    public T consume() {
        if (lastResult == null) {
            throw new IllegalStateException("Last result is empty");
        }

        T value = lastResult;
        lastResult = null;

        return value;
    }
}
