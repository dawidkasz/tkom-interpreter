package executor;

final class ResultStore {
    private Object lastResult = null;

    public void store(Object value) {
        lastResult = value;
    }

    public Object fetchAndReset() {
        if (lastResult == null) {
            throw new IllegalStateException("Last result is empty");
        }

        return lastResult;
    }
}
