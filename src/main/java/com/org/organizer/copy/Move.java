package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Implements the move operation for files. The operation will not replace files.
 * Its file attributes will by default be the same.
 */
public class Move implements ICopy {
    /**
     * Move the file from the source to its destination. If the file already exists,
     * then nothing is done. That means the original file will remain in its directory.
     * If from equals to then an exception will be thrown.
     *
     * @param from source file path
     * @param to destination file path
     * @throws IOException if an error occured while moving the file
     */
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) throw new IOException("file already exists");
        Files.move(from, to);
    }
}
