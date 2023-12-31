package com.org.util.consistency;

import com.org.parser.command.CommandException;
import com.org.parser.Configuration;

import java.io.File;

/**
 * Class that offers some often used validation checks.
 */
public class Checker {
    /**
     * Check that an object is not null. If it is, throw an exception.
     * @param o
     */
    public static void checkNotNull(Object o) {
        if(o == null) throw new IllegalArgumentException("object " + o + "is null");
    }

    /**
     * Check that a string is not empty, or else throw an exception.
     * @param str
     */
    public static void checkStringNotEmpty(String str) {
        if(str.isEmpty()) throw new IllegalArgumentException("string can't be empty");
    }

    /**
     * Check a organizer repository by searching for the property file and
     * the error folder. Both should exist.
     * @param path
     * @throws CommandException when the repo is missing parts
     */
    public static void checkRepository(String path) throws CommandException {
        checkRepositoryFile(path);
        checkErrorFolder(path);
    }

    /**
     * Check that the repository exists by finding the repository property file.
     * @param path
     * @throws CommandException if the config file doesn't exist
     */
    public static void checkRepositoryFile(String path) throws CommandException {
        File repo;
        if(path.isEmpty()) repo = new File(Configuration.PROPERTY_FILE_NAME_STRING);
        else repo = new File(path, Configuration.PROPERTY_FILE_NAME_STRING);
        if(!repo.exists()) throw new CommandException("not a repository");
    }

    /**
     * Check that the error folder exists in the given string path.
     * @param path
     * @throws CommandException if it doesn't exist
     */
    public static void checkErrorFolder(String path) throws CommandException {
        File repo;
        if(path.isEmpty()) repo = new File(Configuration.PROPERTY_FILE_NAME_STRING);
        else repo = new File(path, Configuration.PROPERTY_FILE_NAME_STRING);
        File errorFolder = new File(repo.getParentFile(), Configuration.ERROR_FOLDER_NAME);
        if(!errorFolder.exists()) throw new CommandException("error folder is missing, run this command to fix:\n organizer repair");
    }
}
