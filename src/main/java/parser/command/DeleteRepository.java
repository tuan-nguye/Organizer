package parser.command;

import parser.CommandException;
import parser.Configuration;
import util.consistency.Checker;

import java.io.File;

public class DeleteRepository extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(!Checker.validRepository(Configuration.PROPERTY_FILE_PATH_STRING)) {
            throw new CommandException("can't delete uninitialized repository");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        File propertyFile = new File(Configuration.PROPERTY_FILE_PATH_STRING, Configuration.PROPERTY_FILE_NAME_STRING);
        if(propertyFile.delete()) {
            System.out.println("repository successfully deleted");
        } else {
            System.out.println("repository could not be deleted");
        }
    }
}
