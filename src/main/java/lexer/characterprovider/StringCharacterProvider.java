package lexer.characterprovider;

import lexer.PositionedCharacter;
import lexer.PositionTracker;

import java.util.NoSuchElementException;

public class StringCharacterProvider implements CharacterProvider {
    private final String textInput;
    private final PositionTracker positionTracker = new PositionTracker();
    private int index = 0;

    public StringCharacterProvider(String textInput) {
        this.textInput = textInput;
    }

    @Override
    public PositionedCharacter next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters to read");
        }

        positionTracker.updatePosition(textInput.charAt(index++));

        return positionTracker.getPosition();
    }

    @Override
    public boolean hasNext() {
        return index < textInput.length();
    }
}
