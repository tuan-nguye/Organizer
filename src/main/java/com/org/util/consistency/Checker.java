package com.org.util.consistency;

import com.org.parser.CommandException;
import com.org.parser.Configuration;

import java.io.File;

/*
refactor pre-/postcondition checks
 */
public class Checker {
    public static void checkNotNull(Object o) {
        if(o == null) throw new IllegalArgumentException("object " + o + "is null");
    }

    public static void checkStringNotEmpty(String str) {
        if(str.isEmpty()) throw new IllegalArgumentException("string can't be empty");
    }

    public static void checkRepository(String path) throws CommandException {
        File repo;
        if(path.isEmpty()) repo = new File(Configuration.PROPERTY_FILE_NAME_STRING);
        else repo = new File(path, Configuration.PROPERTY_FILE_NAME_STRING);
        if(!repo.exists()) throw new CommandException("not a repository");

        File errorFolder = new File(repo, Configuration.ERROR_FOLDER_NAME);
        if(errorFolder.exists()) throw new CommandException("error folder is missing, run this command to fix:\n organizer repair");
    }
}
