package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Path;


/**
 * Copy/Move File interface
 */
public interface ICopy {
    /**
     * execute the file operation.
     * @param from source file path
     * @param to destination file path
     * @throws IOException if errors occured during the execution of the operation
     */
    void execute(Path from, Path to) throws IOException;
}
