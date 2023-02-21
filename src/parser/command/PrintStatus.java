package parser.command;

import parser.CommandException;
import parser.Configuration;

public class PrintStatus extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("status command doesn't take in arguments");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        // print status
        // and properties written in config file
    }
}
