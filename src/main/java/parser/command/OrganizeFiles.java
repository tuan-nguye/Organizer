package parser.command;

import parser.CommandException;
import parser.Configuration;

public class OrganizeFiles extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 2) {
            throw new CommandException("invalid number of arguments, needs 2");
        } // check if source and destination exist, destination is valid repo

    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        System.out.println("source=" + args[0]);
        System.out.println("destination=" + args[1]);
    }
}
