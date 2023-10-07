package organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Move implements ICopy {
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) return;
        Files.move(from, to);
    }
}
