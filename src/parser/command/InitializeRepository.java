package parser.command;

import parser.Configuration;
import parser.option.Option;
import parser.option.ValueOption;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class InitializeRepository extends Command {

    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        File propertiesFile = new File(Configuration.PROPERTY_FILE_PATH_STRING);

        if(propertiesFile.exists()) {
            System.err.println("organizer repository already initialized");
            return false;
        } else {
            try {
                propertiesFile.createNewFile();
            } catch(IOException ioe) {
                System.err.println("error occurred during creating config file");
                return false;
            }
        }

        return true;
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        int folderSize = getFolderSize(config);
        System.out.println("initializing repo with size=" + folderSize + "... beep beep boop");
    }

    private int getFolderSize(Configuration config) {
        Map<String, Option> optionMap = config.allOptions();
        ValueOption folderSizeOption = (ValueOption) optionMap.get("folderSize");
        return Integer.parseInt(folderSizeOption.getValues().get(0));
    }
}
