package parser;

import parser.command.Command;
import parser.option.Option;

import java.util.*;

public class CommandLineParser {
    // all available commands and options mapped to their name
    Map<String, Command> commandMap = new HashMap<>();
    Map<String, Option> optionMap = new HashMap<>();

    public CommandLineParser() {}

    public String[] parse(String[] args) throws IllegalArgumentException {
        List<String> commandArguments = new ArrayList<>();

        for(String arg : args) {
            if(arg.startsWith(Option.PREFIX)) {
                String strOption = arg.substring(2);
                int idxAssign = strOption.indexOf('=');
                String optionName;
                if(idxAssign != -1) optionName = strOption.substring(0, idxAssign);
                else optionName = strOption;
                if(!optionMap.containsKey(optionName)) throw new IllegalArgumentException("no option '" + optionName + "'");
                Option option = optionMap.get(optionName);
                option.parseArguments(strOption);
            } else {
                commandArguments.add(arg);
            }
        }

        return commandArguments.toArray(new String[0]);
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

/*
java -jar name.jar [command] [--option1 --option2 ... --optionN]

init --size=500
copy /source /destination --skip

 */