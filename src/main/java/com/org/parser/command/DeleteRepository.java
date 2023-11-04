package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.consistency.Checker;

import java.io.File;

public class DeleteRepository extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
    }

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
