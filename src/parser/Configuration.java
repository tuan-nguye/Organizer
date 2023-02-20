package parser;

import parser.command.*;
import parser.option.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Configuration {
    /* file where properties are stored */
    public static final String PROPERTY_FILE_PATH_STRING = ".organizer_config.txt";

    /* available commands */
    Command organizeCommand = new CopyFiles()
            .setName("organize")
            .setDescription("copy and organize all files according to their time stamp")
            .setCommandFormat("organize /path/to/source /path/to/destination");
    Command initCommand = new InitializeRepository()
            .setName("init")
            .setDescription("initialize organizer destination directory, default folder size is 500")
            .setCommandFormat("init [--folderSize=500]");
    Command statusCommand = new PrintStatus()
            .setName("status")
            .setDescription("print status information in console");

    Command helpCommand = new PrintHelp()
            .setName("help")
            .setDescription("print help text");

    Command versionCommand = new PrintVersion()
            .setName("version")
            .setDescription("organizer version 0.1");

    /* available options */
    Option folderSizeOption = new ValueOption()
            .allowAllValues(true)
            .defaultValue("500")
            .setName("folderSize")
            .setDescription("set max folder size threshold")
            .setOptionFormat("--folderSize=N");
    Option skipOption = new FlagOption()
            .setName("skip")
            .setDescription("skip files that already exist instead of replacing them");

    //Option cutOption = new FlagOption(); // cut instead of copy, kinda dangerous
    Option fileExtensionsOption = new ValueOption()
            .allowAllValues(true)
            .setName("fileExtensions")
            .setDescription("constrain allowed file extensions")
            .setOptionFormat("--fileExtensions=[jpg, jpeg, png, txt, ...]");

    public Map<String, Command> allCommands() {
        Map<String, Command> allCommands = new HashMap<>();
        allCommands.put(organizeCommand.getName(), organizeCommand);
        allCommands.put(initCommand.getName(), initCommand);
        allCommands.put(statusCommand.getName(), statusCommand);
        allCommands.put(helpCommand.getName(), helpCommand);
        allCommands.put(versionCommand.getName(), versionCommand);
        return allCommands;
    }

    public Map<String, Option> allOptions() {
        Map<String, Option> allOptions = new HashMap<>();
        allOptions.put(folderSizeOption.getName(), folderSizeOption);
        allOptions.put(skipOption.getName(), skipOption);
        allOptions.put(fileExtensionsOption.getName(), fileExtensionsOption);
        return allOptions;
    }

    public List<Option> optionsStoredInPropertyFile() {
        List<Option> options = new ArrayList<>();
        options.add(folderSizeOption);
        return options;
    }

    /**
     * read properties from hidden property file
     * @return returns properties map, if the file doesn't exist or is empty, the map is empty
     */
    public Properties readProperties() {
        Properties properties = new Properties();

        try {
            File propertyFile = new File(PROPERTY_FILE_PATH_STRING);
            FileInputStream in = new FileInputStream(propertyFile);
            properties.load(in);
            in.close();
        } catch(FileNotFoundException fnfe) {
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return properties;
    }
}
