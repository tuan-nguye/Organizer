package com.org.parser;

import com.org.parser.command.Command;
import com.org.parser.option.Option;

import java.util.*;

/**
 * The commandLineParser parses the entire line that was entered in the command line.
 * It splits off the command, its arguments, and all additional options and saves the
 * configuration in a map. If a command or option was given that doesn't exist,
 * then a ParseException will be thrown.
 */
public class CommandLineParser {
    // all available commands and options objects mapped to their name
    Map<String, Command> commandMap = new HashMap<>();
    Map<String, Option> optionMap = new HashMap<>();

    /**
     * Default empty constructor.
     */
    public CommandLineParser() {}

    /**
     * parse command line to get command, arguments and options
     * @param args
     * @return command and additional arguments in a string array for later execution
     * @throws ParseException throws exception on invalid configuration
     */
    public String[] parse(String[] args) throws ParseException {
        // list with the command in the 0th place and all arguments in subsequent
        List<String> commandArguments = new ArrayList<>();

        // go through each argument
        for(String arg : args) {
            // if the argument starts with the typical option prefix '--'
            // it's an option else it's a command argument
            if(arg.startsWith(Option.PREFIX)) {
                // split off the option's name by first removing the prefix
                String strOption = arg.substring(2);
                // value options have an equal sign, e.g. name=value
                int idxAssign = strOption.indexOf('=');
                String optionName;
                if(idxAssign != -1) optionName = strOption.substring(0, idxAssign);
                else optionName = strOption;
                // the option doesn't exist if it doesn't appear in the option map
                if(!optionMap.containsKey(optionName)) {
                    throw new ParseException(String.format("option %s doesn't exist", optionName));
                }
                // get the option object and parse the arguments
                Option option = optionMap.get(optionName);
                option.parseArguments(strOption);
            } else {
                // add the command or one of its arguments to the list
                commandArguments.add(arg);
            }
        }

        // validate the command
        validateCommand(commandArguments);
        return commandArguments.toArray(new String[0]);
    }

    /**
     * Validate the input command with all commands that exist.
     * @param commandArguments a list with the command in the first position and its arguments in the subsequent
     * @throws ParseException if no command is given or the command doesn't exist
     */
    private void validateCommand(List<String> commandArguments) throws ParseException {
        // throw exception when no command was given
        if(commandArguments.isEmpty()) {
            throw new ParseException("command is missing");
        }

        // throw exception when given command doesn't exist
        String command = commandArguments.get(0);
        if(!commandMap.containsKey(command)) {
            throw new ParseException(String.format("command '%s' doesn't exist", command));
        }
    }

    /**
     * Add the option names mapped to the object to the map storing all options.
     * @param opts option map
     */
    public void addOption(Map<String, Option> opts) {
        optionMap.putAll(opts);
    }

    /**
     * Add a single option to the map. The key is the name stored in the option.
     * @param option option object
     */
    public void addOption(Option option) {
        optionMap.put(option.getName(), option);
    }

    /**
     * Add the command names mapped to the object to the map storing all commands.
     * @param comms command map
     */
    public void addCommand(Map<String, Command> comms) {
        commandMap.putAll(comms);
    }

    /**
     * Add a single command to the map. The key is stored in the command object.
     * @param command
     */
    public void addCommand(Command command) {
        commandMap.put(command.getName(), command);
    }
}