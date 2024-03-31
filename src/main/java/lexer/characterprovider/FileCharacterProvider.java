package lexer.characterprovider;

import lexer.PositionedCharacter;
import lexer.PositionTracker;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

public class FileCharacterProvider implements CharacterProvider, AutoCloseable {
    private final InputStreamReader fileReader;
    private final PositionTracker positionTracker = new PositionTracker();
    private char nextChar;
    private boolean isEndOfFile = false;

    public FileCharacterProvider(String filePath) {
        try {
            this.fileReader = new FileReader(filePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }

        advance();
    }

    @Override
    public boolean hasNext() {
        return !isEndOfFile;
    }

    @Override
    public PositionedCharacter next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more characters to read");
        }

        char currentChar = this.nextChar;
        advance();

        positionTracker.updatePosition(currentChar);

        return positionTracker.getPosition();
    }

    @Override
    public void close() throws IOException {
        fileReader.close();
    }

    private void advance() {
        try {
            int nextCharCode = this.fileReader.read();

            if (nextCharCode == -1) {
                this.isEndOfFile = true;
                close();
                return;
            }

            this.nextChar = (char) nextCharCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
