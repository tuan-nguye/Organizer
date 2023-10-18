package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class InitializeRepository extends Command {
    public static final int DEFAULT_FOLDER_SIZE = 1000;

    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length >= 2) {
            throw new CommandException("too many arguments, needs 0 or 1");
        }

        File propertiesFile = new File(config.PROPERTY_FILE_PATH_STRING, Configuration.PROPERTY_FILE_NAME_STRING);

        if(propertiesFile.exists()) {
            throw new CommandException("repository already initialized");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        Properties properties = config.getProperties();
        int folderSize = DEFAULT_FOLDER_SIZE;
        if(args.length != 0) {
            folderSize = Integer.parseInt(args[0]);
        }

        String fullPath = Configuration.PROPERTY_FILE_NAME_STRING;
        if(!config.PROPERTY_FILE_PATH_STRING.isEmpty()) fullPath = config.PROPERTY_FILE_PATH_STRING + File.separator + fullPath;
        System.out.println("initializing repo with size=" + folderSize + "...");
        File propertiesFile = new File(fullPath);
        File errorFolder = new File(config.PROPERTY_FILE_PATH_STRING + File.separator + Configuration.ERROR_FOLDER_NAME);

        try {
            if(!propertiesFile.createNewFile()) {
                System.err.println("file already exists, should never come here");
            }
            FileTools.setFileVisibility(propertiesFile, true);
            errorFolder.mkdir();
        } catch(IOException ioe) {
            System.err.println("ioexception when creating properties file");
        }

        properties.put("folderSize", String.valueOf(folderSize));
        FileTools.storeProperties(properties, fullPath);

        System.out.println("repo created successfully");
    }
}
