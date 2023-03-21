package organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Move implements ICopy {
    @Override
    public void copy(Path from, Path to) throws IOException {
        Files.move(from, to);
    }
}
