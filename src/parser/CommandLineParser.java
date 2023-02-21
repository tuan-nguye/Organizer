package parser;

import parser.command.Command;
import parser.option.Option;

import java.util.*;

public class CommandLineParser {
    // all available commands and options mapped to their name
    Map<String, Command> commandMap = new HashMap<>();
    Map<String, Option> optionMap = new HashMap<>();

    public CommandLineParser() {}

    /**
     * parse command line to get command, arguments and options
     * @param args
     * @return command and additional arguments in a string array,
     *          throws exception on invalid configuration
     */
    public String[] parse(String[] args) throws ParseException {
        List<String> commandArguments = new ArrayList<>();

        for(String arg : args) {
            if(arg.startsWith(Option.PREFIX)) {
                String strOption = arg.substring(2);
                int idxAssign = strOption.indexOf('=');
                String optionName;
                if(idxAssign != -1) optionName = strOption.substring(0, idxAssign);
                else optionName = strOption;
                if(!optionMap.containsKey(optionName)) {
                    throw new ParseException(String.format("option %s doesn't exist", optionName));
                }
                Option option = optionMap.get(optionName);
                option.parseArguments(strOption);
            } else {
                commandArguments.add(arg);
            }
        }

        validateCommand(commandArguments);
        return commandArguments.toArray(new String[0]);
    }

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

    public void addOption(Map<String, Option> opts) {
        optionMap.putAll(opts);
    }

    public void addOption(Option option) {
        optionMap.put(option.getName(), option);
    }

    public void addCommand(Map<String, Command> comms) {
        commandMap.putAll(comms);
    }

    public void addCommand(Command command) {
        commandMap.put(command.getName(), command);
    }
}