package lexer;

public class StringCharacterProvider implements CharacterProvider {
    private final String textInput;
    private int index = 0;

    public StringCharacterProvider(String textInput) {
        this.textInput = textInput;
    }

    @Override
    public Character next() {
        return textInput.charAt(index++);
    }

    @Override
    public boolean hasNext() {
        return index < textInput.length();
    }
}
