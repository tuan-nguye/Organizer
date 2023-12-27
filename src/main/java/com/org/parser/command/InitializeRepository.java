package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Command for initializing a repository.
 */
public class InitializeRepository extends Command {
    // folder size threshold
    public static final int DEFAULT_FOLDER_SIZE = 1000;

    /**
     * validate that exactly 0 or 1 argument is given. The first argument must be the
     * folder size threshold. If none is given the default size is used. If the repo
     * is already initialized, then an exception will be thrown.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if the number of arguments is incorrect or the repo already exists
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // this command only accepts the folder threshold as optional argument
        if(args.length >= 2) {
            throw new CommandException("too many arguments, needs 0 or 1");
        } else if(args.length == 1) {
            try {
                Integer.parseInt(args[0]);
            } catch(NumberFormatException nfe) {
                throw new CommandException("folder threshold needs to be an integer");
            }
        }

        File propertiesFile = new File(config.PROPERTY_FILE_PATH_STRING, Configuration.PROPERTY_FILE_NAME_STRING);
        // if the property file exists, there already is a repo in this directory
        if(propertiesFile.exists()) {
            throw new CommandException("repository already initialized");
        }
    }

    /**
     * Initialize the repository with a folder threshold by creating a config file
     * and writing properties to it.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // use the default size or the one given in the argument
        Properties properties = config.getProperties();
        int folderSize = DEFAULT_FOLDER_SIZE;
        if(args.length != 0) {
            folderSize = Integer.parseInt(args[0]);
        }

        // get the repo path from the configuration object
        String fullPath = Configuration.PROPERTY_FILE_NAME_STRING;
        if(!config.PROPERTY_FILE_PATH_STRING.isEmpty()) fullPath = config.PROPERTY_FILE_PATH_STRING + File.separator + fullPath;
        System.out.println("initializing repo with size=" + folderSize + "...");
        // create the files for the property file and the error folder
        File propertiesFile = new File(fullPath);
        File errorFolder = new File(config.PROPERTY_FILE_PATH_STRING + File.separator + Configuration.ERROR_FOLDER_NAME);

        // attempt to create the files
        try {
            if(!propertiesFile.createNewFile()) {
                System.err.println("file already exists, should never come here");
            }
            FileTools.setFileVisibility(propertiesFile, true);
            errorFolder.mkdir();
        } catch(IOException ioe) {
            System.err.println("ioexception when creating properties file");
        }

        // write the properties to the file
        properties.put("folderSize", String.valueOf(folderSize));
        FileTools.storeProperties(properties, fullPath);

        System.out.println("repo created successfully");
    }
}
