package organizer.copy;

import java.io.IOException;
import java.nio.file.Path;

public interface ICopy {
    void execute(Path from, Path to) throws IOException;
}
