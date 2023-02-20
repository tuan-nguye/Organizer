package parser.command;

import parser.Configuration;

public class PrintVersion extends Command {

    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        if(args.length != 0) {
            System.err.println("print command doesn't take any arguments");
            return false;
        }

        return true;
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        System.out.println(getDescription());
    }
}
