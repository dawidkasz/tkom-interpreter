package lexer.characterprovider;

import lexer.PositionedCharacter;
import lexer.TextPositionTracker;

import java.util.NoSuchElementException;

public class StringCharacterProvider implements CharacterProvider {
    private final String textInput;
    private final TextPositionTracker tracker = new TextPositionTracker();
    private int index = 0;

    public StringCharacterProvider(String textInput) {
        this.textInput = textInput;
    }

    @Override
    public PositionedCharacter next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters to read");
        }

        tracker.updatePosition(textInput.charAt(index++));

        return tracker.getPosition();
    }

    @Override
    public boolean hasNext() {
        return index < textInput.length();
    }
}
