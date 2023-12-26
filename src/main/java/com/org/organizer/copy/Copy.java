package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * Standard copy operation without any additional features. The file's attributes
 * and metadata are also copied by default.
 */
public class Copy implements ICopy {
    /**
     * Copy the 'from' file and copy it to the 'to' file path. If the file
     * already exists, then nothing is done. If from equals to then an exception
     * will be thrown.
     *
     * @param from source file path
     * @param to destination file path
     * @throws IOException if an error occured during the copy operation
     */
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) throw new IOException("file already exists");
        Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
    }
}
