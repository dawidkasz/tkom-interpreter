package lexer;

public class PositionTracker {
    private int currentLine = 1;
    private int currentColumn = 1;
    private Character previousChar = null;

    public void updatePosition(Character nextChar) {  // \r\n
        if (previousChar == null) {
            previousChar = nextChar;
            return;
        }

        if (previousChar == '\n') {
            currentLine++;
            currentColumn = 1;
        } else {
            currentColumn++;
        }

        previousChar = nextChar;
    }

    public PositionedCharacter getPosition() {
        return new PositionedCharacter(previousChar, new Position(currentLine, currentColumn));
    }
}
