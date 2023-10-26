package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.consistency.Checker;

import java.util.Map;
import java.util.Properties;

public class PrintStatus extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 0) {
            throw new CommandException("status command doesn't take in arguments");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        try {
            Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
        } catch(CommandException ce) {
            System.err.println("not a valid repository");
            return;
        }

        System.out.println("valid repository");
        Properties properties = config.getProperties();

        System.out.println("Properties:");
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            System.out.printf("%s=%s\n", entry.getKey(), entry.getValue());
        }
    }
}
