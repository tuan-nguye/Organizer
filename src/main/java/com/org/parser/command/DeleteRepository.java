package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.consistency.Checker;

import java.io.File;

public class DeleteRepository extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(!Checker.validRepository(config.PROPERTY_FILE_PATH_STRING)) {
            throw new CommandException("can't delete uninitialized repository");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        File propertyFile = new File(config.PROPERTY_FILE_PATH_STRING, Configuration.PROPERTY_FILE_NAME_STRING);
        if(propertyFile.delete()) {
            System.out.println("repository successfully deleted");
        } else {
            System.out.println("repository could not be deleted");
        }
    }
}
