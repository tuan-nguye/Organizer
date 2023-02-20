package parser.command;

import parser.Configuration;

public class CopyFiles extends Command {
    @Override
    public void execute(String[] args, Configuration config) {
        if(args.length != 2) throw new IllegalArgumentException("source or destination missing");
        System.out.println("source=" + args[0]);
        System.out.println("destination=" + args[1]);
    }
}
