package parser;

import parser.command.Command;
import parser.option.Option;

import java.util.*;

public class CommandLineParser {
    // all available commands and options mapped to their name
    Map<String, Command> commandMap = new HashMap<>();
    Map<String, Option> optionMap = new HashMap<>();

    public CommandLineParser() {}

    public Command parse(String[] args) throws IllegalArgumentException {
        List<String> commandArguments = new ArrayList<>();

        for(String arg : args) {
            if(arg.startsWith(Option.PREFIX)) {
                String strOption = arg.substring(2);
                if(!optionMap.containsKey(strOption)) throw new IllegalArgumentException("no option '" + strOption + "'");
                Option option = optionMap.get(strOption);
                option.parseArguments(strOption);
            } else {
                commandArguments.add(arg);
            }
        }

        return getCommand(commandArguments);
    }

    private Command getCommand(List<String> commandArgs) throws IllegalArgumentException {
        String strCommand = commandArgs.remove(0);
        if(!commandMap.containsKey(strCommand)) throw new IllegalArgumentException("no command '" + strCommand + "'");
        Command command = commandMap.get(strCommand);
        return command;
    }

    public void addOption(Collection<Option> opts) {
        for(Option opt : opts) {
            addOption(opt);
        }
    }

    public void addOption(Option option) {
        optionMap.put(option.getName(), option);
    }

    public void addCommand(Collection<Command> comms) {
        for(Command comm : comms) {
            addCommand(comm);
        }
    }

    public void addCommand(Command command) {
        commandMap.put(command.getName(), command);
    }
}

/*
java -jar name.jar [command] [--option1 --option2 ... --optionN]

init --size=500
copy /source /destination --skip

 */