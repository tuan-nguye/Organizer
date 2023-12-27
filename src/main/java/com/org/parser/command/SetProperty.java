package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.consistency.Checker;
import com.org.util.FileTools;

import java.util.Properties;

/**
 * This command can modify properties of the repository. Multiple properties
 * can be set with one execution
 */
public class SetProperty extends Command {
    /**
     * Validate that the repository is valid and a valid key-value pair was given
     * to update the property.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if arguments are invalid, repo doesn't exist
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // check that arguments were given
        if(args.length == 0) {
            throw new CommandException("property arguments missing");
        }

        // check that the repo exists
        Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);

        // get the given property's key value pair by splitting it between the equal sign
        String property = args[0].substring(0, args[0].indexOf('='));

        // check that the property exists and it can be modified
        if(!config.getPropertyNames().contains(property)) {
            throw new CommandException(String.format("property '%s' unknown", property));
        } else if(!config.getModifiableProperties().contains(property)) {
            throw new CommandException(String.format("property '%s' can't be modified", property));
        }
    }

    /**
     * Go through each argument, split the key value pairs and modify the configuration
     * property file by writing the updated values to it.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // get all properties before the change
        Properties properties = config.getProperties();

        // iterate through all properties in the input
        for(String arg : args) {
            // the input has to look like this key=value
            // if not, then the input is incorrectly formatted
            int idxAssign = arg.indexOf('=');
            if(idxAssign == -1 || idxAssign == arg.length()-1) {
                System.err.println("value is missing for property " + arg);
                return;
            }

            // split the property name and the value and update the properties with its new values
            String name = arg.substring(0, idxAssign), value = arg.substring(idxAssign+1);
            properties.put(name, value);
        }

        // write the changes to the property file
        FileTools.storeProperties(properties, Configuration.PROPERTY_FILE_NAME_STRING);
    }
}
