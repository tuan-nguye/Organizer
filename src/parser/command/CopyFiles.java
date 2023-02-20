package parser.command;

import parser.Configuration;

public class CopyFiles extends Command {
    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        if(args.length != 2) {
            System.err.println("invalid number of arguments");
            System.err.println("usage: " + getCommandFormat());
            return false;
        } // check if source and destination exist, destination is valid repo

        return true;
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        if(args.length != 2) throw new IllegalArgumentException("source or destination missing");
        System.out.println("source=" + args[0]);
        System.out.println("destination=" + args[1]);
    }
}
