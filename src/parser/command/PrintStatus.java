package parser.command;

import parser.Configuration;

public class PrintStatus extends Command {
    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        if(args.length != 0) {
            System.err.println("status command doesn't take in arguments");
            return false;
        }

        return true;
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        // print status
        // and properties written in config file
    }
}
