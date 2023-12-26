package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * This class implements the operation that copies and replaces a file.
 */
public class CopyReplace implements ICopy {
    /**
     * Copy a file from the 'from' file path to the 'to' file path. If the file already
     * exists, then replace it with a new file. If from equals to then an exception
     * will be thrown.
     *
     * @param from source file path
     * @param to destination file path
     * @throws IOException if an error occurs during copying.
     */
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) throw new IOException("file already exists");
        Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }
}
