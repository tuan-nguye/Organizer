package com.org.parser.command;

import com.org.parser.Configuration;

/**
 * This command simply prints the current version of the application.
 */
public class PrintVersion extends Command {

    /**
     * Check that no arguments were given
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if there are arguments
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("printing version command doesn't take any arguments");
        }
    }

    /**
     * print the current version of the application in the console
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // the current version is stored in the command's description
        System.out.println(getDescription());
    }
}
