package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.consistency.Checker;

import java.util.Map;
import java.util.Properties;

/**
 * This command prints the status of a repository by checking whether the repo exists
 * and print all existing properties.
 */
public class PrintStatus extends Command {
    /**
     * Check that the configuration is correct. This command doesn't accept arguments.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if arguments were given
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // no arguments allowed
        if(args.length != 0) {
            throw new CommandException("status command doesn't take in arguments");
        }
    }

    /**
     * Check if the repo exists. If it does, then print all properties.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // check repository existence
        try {
            Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
            return;
        }

        // the repo exists, so there should be a config file with all properties
        System.out.println("valid repository");
        Properties properties = config.getProperties();

        // print the properties in the console
        System.out.println("Properties:");
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            System.out.printf("%s=%s\n", entry.getKey(), entry.getValue());
        }
    }
}
