package com.org.organizer.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MoveReplace implements ICopy {
    @Override
    public void execute(Path from, Path to) throws IOException {
        if(from.equals(to)) throw new IOException("file already exists");
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}