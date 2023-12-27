package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.consistency.Checker;

import java.io.File;

/**
 * Command to delete an existing repository in the current working directory.
 */
public class DeleteRepository extends Command {
    /**
     * validate the repository actually exists. The repository exists if
     * the hidden configuration file can be found in the directory.
     * If it doesn't, then throw an exception.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if the repo doesn't exist
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
    }

    /**
     * Delete the hidden property file and the error folder associated to the repository.
     * When they're deleted the repository doesn't exist anymore. All other files will
     * remain. This function only works if the error folder is empty.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        File propertyFile = new File(config.PROPERTY_FILE_PATH_STRING, Configuration.PROPERTY_FILE_NAME_STRING);
        File errorFolder = new File(config.PROPERTY_FILE_PATH_STRING, Configuration.ERROR_FOLDER_NAME);
        if(propertyFile.delete() && errorFolder.delete()) {
            System.out.println("repository successfully deleted");
        } else {
            System.out.println("repository could not be deleted");
        }
    }
}
