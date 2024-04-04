package lexer;

public record PositionedCharacter(Character character, Position position) {
    @Override
    public String toString() {
        return character.toString();
    }
}
