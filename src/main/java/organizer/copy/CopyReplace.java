package organizer.copy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CopyReplace implements ICopy {
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) return;
        Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }
}
