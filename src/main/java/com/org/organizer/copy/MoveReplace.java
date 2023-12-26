package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * Implements the operation to move and replace a file.
 */
public class MoveReplace implements ICopy {
    /**
     * Move a file from its source to the destination. If the file already exists,
     * then replace the file with the new one. When no exceptions occur, it's
     * guaranteed that the file in 'from' will be gone.
     *
     * @param from source file path
     * @param to destination file path
     * @throws IOException
     */
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) throw new IOException("file already exists");
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}
