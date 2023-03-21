package organizer.copy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Copy implements ICopy {
    @Override
    public void copy(Path from, Path to) throws IOException {
        try {
            Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
        } catch(FileAlreadyExistsException ignored) {}
    }
}
