package organizer.copy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Move implements ICopy {
    @Override
    public void copy(Path from, Path to) throws IOException {
        try {
            Files.move(from, to);
        } catch(FileAlreadyExistsException ignored) {}
    }
}
