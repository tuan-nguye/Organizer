package parser.command;

import parser.CommandException;
import parser.Configuration;
import util.FileTools;

import java.io.File;
import java.util.Properties;

public class SetProperty extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length == 0) {
            throw new CommandException("property arguments missing");
        }

        File propertyFile = new File(Configuration.PROPERTY_FILE_NAME_STRING);
        if(!propertyFile.exists()) {
            throw new CommandException("can't set property, repository not initialized");
        }

        String property = args[0].substring(0, args[0].indexOf('='));

        if(!config.getPropertyNames().contains(property)) {
            throw new CommandException(String.format("property '%s' unknown", property));
        } else if(!config.getModifiableProperties().contains(property)) {
            throw new CommandException(String.format("property '%s' can't be modified", property));
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        Properties properties = config.getProperties();

        for(String arg : args) {
            int idxAssign = arg.indexOf('=');
            if(idxAssign == -1) {
                System.err.println("value is missing for property " + arg);
                return;
            }

            String name = arg.substring(0, idxAssign), value = arg.substring(idxAssign+1);
            properties.put(name, value);
        }

        FileTools.storeProperties(properties, Configuration.PROPERTY_FILE_NAME_STRING);
    }
}
