package parser.command;

import parser.Configuration;
import util.FileTools;

import java.io.File;
import java.util.Properties;

public class SetProperty extends Command {
    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        if(args.length == 0) {
            System.err.println("property arguments missing");
            return false;
        }

        File propertyFile = new File(Configuration.PROPERTY_FILE_PATH_STRING);
        if(!propertyFile.exists()) {
            System.err.println("can't set property, repository not initialized");
            return false;
        }

        return true;
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

        FileTools.storeProperties(properties, Configuration.PROPERTY_FILE_PATH_STRING);
    }
}
