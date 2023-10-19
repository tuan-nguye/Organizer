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
        if(!Checker.validRepository(config.PROPERTY_FILE_PATH_STRING)) {
            System.out.println("not an organizer repository");
            return;
        } else {
            System.out.println("organizer repository initialized\n");
        }

        Properties properties = config.getProperties();

        System.out.println("Properties:");
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            System.out.printf("%s=%s\n", entry.getKey(), entry.getValue());
        }
    }
}
