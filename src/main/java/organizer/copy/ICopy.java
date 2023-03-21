package organizer.copy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public interface ICopy {
    void copy(Path from, Path to) throws IOException;
}
