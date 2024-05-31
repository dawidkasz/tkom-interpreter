package executor;

final class ResultStore<T> {
    private T lastResult = null;

    public void store(T value) {
        lastResult = value;
    }

    public T fetchAndReset() {
        if (lastResult == null) {
            throw new IllegalStateException("Last result is empty");
        }

        return lastResult;
    }
}
