package parser.command;

import parser.Configuration;

public class PrintVersion extends Command {
    @Override
    public void execute(String[] args, Configuration config) {
        System.out.println(getDescription());
    }
}
