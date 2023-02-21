package parser.command;

import parser.Configuration;
import parser.option.Option;
import parser.option.ValueOption;
import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

public class InitializeRepository extends Command {
    public static final int DEFAULT_FOLDER_SIZE = 500;

    @Override
    public boolean validateConfiguration(String[] args, Configuration config) {
        if(args.length >= 2) {
            System.err.println("too many arguments");
            return false;
        }

        File propertiesFile = new File(Configuration.PROPERTY_FILE_PATH_STRING);

        if(propertiesFile.exists()) {
            System.err.println("organizer repository already initialized");
            return false;
        }

        return true;
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        Properties properties = config.getProperties();
        int folderSize = DEFAULT_FOLDER_SIZE;
        if(args.length != 0) {
            folderSize = Integer.parseInt(args[0]);
        }
        System.out.println("initializing repo with size=" + folderSize + "... beep beep boop");
        File propertiesFile = new File(Configuration.PROPERTY_FILE_PATH_STRING);

        try {
            if(!propertiesFile.createNewFile()) {
                System.err.println("file already exists, should never come here");
            }
            FileTools.setFileVisibility(propertiesFile, true);
        } catch(IOException ioe) {
            System.err.println("ioexception when creating properties file");
        }

        properties.put("folderSize", String.valueOf(folderSize));
        FileTools.storeProperties(properties, Configuration.PROPERTY_FILE_PATH_STRING);
    }
}
