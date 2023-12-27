package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.parser.option.Option;

import java.util.Collection;
import java.util.Map;

/**
 * Command to print all commands and available options with their respective
 * descriptions.
 */
public class PrintHelp extends Command {
    /**
     * This command doesn't take in arguments
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if arguments are given
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("help command doesn't take in arguments");
        }
    }

    /**
     * Build a table with all commands, featuring their name, description, and format.
     * Also list all available options with their description and print it to the console.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // print all commands
        System.out.println("LIST OF COMMANDS:");
        System.out.println();

        // get all commands from the configuration
        Map<String, Command> commandMap = config.allCommands();
        // get the longest command string length
        int maxCommandLength = getMaxStringLength(commandMap.keySet());
        // print all commands and add padding when needed for nicer format
        for(Command command : commandMap.values()) {
            System.out.printf("%s\t | %s\n", padRight(command.getName(), maxCommandLength), command.getDescription());
            String usage = command.getCommandFormat();
            if(usage != null) System.out.printf("%s\t | usage: %s\n", padRight("", maxCommandLength), usage);
        }

        // print all options
        System.out.println();
        System.out.println("LIST OF OPTIONS:");
        System.out.println();

        // get all options from the configuration object
        Map<String, Option> optionMap = config.allOptions();
        // get the longest option string name again for padding
        int maxOptionLength = getMaxStringLength(optionMap.keySet());
        // print all commands and add padding when needed
        for(Option option : config.allOptions().values()) {
            System.out.printf("%s\t | %s\n", padRight(option.getName(), maxOptionLength), option.getDescription());
            String usage = option.getOptionFormat();
            if(usage != null) System.out.printf("%s\t | usage: %s\n", padRight("", maxOptionLength), usage);
        }
    }

    /**
     * This function iterates through all strings in the collection and returns
     * the length of the longest string.
     * @param coll collection of strings
     * @return maximum length of a single string
     */
    private int getMaxStringLength(Collection<String> coll) {
        int max = 0;
        for(String str : coll) max = Math.max(max, str.length());
        return max;
    }

    /**
     * Add space character padding to the right of an input string, so that the
     * resulting string has a certain total length.
     * @param str input string to be padded
     * @param padding number of characters the output string should have
     * @return the padded string
     */
    private String padRight(String str, int padding) {
        return String.format("%-" + padding + "s", str);
    }
}
