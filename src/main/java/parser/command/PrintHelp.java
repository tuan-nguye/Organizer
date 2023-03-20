package parser.command;

import parser.CommandException;
import parser.Configuration;
import parser.option.Option;

import java.util.Collection;
import java.util.Map;

public class PrintHelp extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("help command doesn't take in arguments");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        // print commands
        System.out.println("LIST OF COMMANDS:");
        System.out.println();

        Map<String, Command> commandMap = config.allCommands();
        int maxCommandLength = getMaxStringLength(commandMap.keySet());
        for(Command command : commandMap.values()) {
            System.out.printf("%s\t | %s\n", padRight(command.getName(), maxCommandLength), command.getDescription());
            String usage = command.getCommandFormat();
            if(usage != null) System.out.printf("%s\t | usage: %s\n", padRight("", maxCommandLength), usage);
        }

        // print options
        System.out.println();
        System.out.println("LIST OF OPTIONS:");
        System.out.println();

        Map<String, Option> optionMap = config.allOptions();
        int maxOptionLength = getMaxStringLength(optionMap.keySet());
        for(Option option : config.allOptions().values()) {
            System.out.printf("%s\t | %s\n", padRight(option.getName(), maxOptionLength), option.getDescription());
            String usage = option.getOptionFormat();
            if(usage != null) System.out.printf("%s\t | usage: %s\n", padRight("", maxOptionLength), usage);
        }
    }

    private int getMaxStringLength(Collection<String> coll) {
        int max = 0;
        for(String str : coll) max = Math.max(max, str.length());
        return max;
    }

    private String padRight(String str, int padding) {
        return String.format("%-" + padding + "s", str);
    }
}
