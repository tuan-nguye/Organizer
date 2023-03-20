package parser.command;

import parser.CommandException;
import parser.Configuration;

public class PrintVersion extends Command {

    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("printing version command doesn't take any arguments");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        System.out.println(getDescription());
    }
}
