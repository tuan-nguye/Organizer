package com.org.parser;

import com.org.parser.command.*;
import com.org.parser.option.FlagOption;
import com.org.parser.option.Option;
import com.org.parser.option.ValueOption;
import com.org.util.FileTools;

import java.io.File;
import java.util.*;

/**
 * This class stores the current configuration with which the command is executed
 * with. It stores all available commands, options, and various info like their
 * names, description, etc. It also gives access to the properties of a repository.
 */
public class Configuration {
    /* file where properties are stored */
    // name of the property file
    public static String PROPERTY_FILE_NAME_STRING = ".organizer_config.txt";
    public static String ERROR_FOLDER_NAME = "error";

    // path to the repository and to the error folder
    public String PROPERTY_FILE_PATH_STRING = "";

    // set of all available properties
    Set<String> propertyNames = new HashSet<>(Arrays.asList("folderSize"));
    // set of all properties that can be modified
    Set<String> modifiableProperties = new HashSet<>(Arrays.asList("folderSize"));

    /* available commands with name, description, etc. */
    Command organizeCommand = new OrganizeFiles()
            .setName("organize")
            .setDescription("copy and organize all files according to their time stamp into the repository in the current working directory")
            .setCommandFormat("organize /path/to/source");
    Command initCommand = new InitializeRepository()
            .setName("init")
            .setDescription("initialize organizer destination directory, default folder size is 500")
            .setCommandFormat("init [folderSize]");
    Command statusCommand = new PrintStatus()
            .setName("status")
            .setDescription("print status information in console");

    Command helpCommand = new PrintHelp()
            .setName("help")
            .setDescription("print help text");

    Command versionCommand = new PrintVersion()
            .setName("version")
            .setDescription("organizer version 0.3");

    Command setPropertyCommand = new SetProperty()
            .setName("setProperty")
            .setDescription("set and store property, currently available properties: folderSize=N: set folder size threshold")
            .setCommandFormat("setProperty [property=value]");

    Command deleteRepository = new DeleteRepository()
            .setName("delete")
            .setDescription("delete repo in current working directory if it exists");

    Command checkCommand = new CheckCommand()
            .setName("check")
            .setDescription("check whether the repository structure is consistent");

    Command repairCommand = new RepairCommand()
            .setName("repair")
            .setDescription("repair the structure if there are any errors");

    Command markCommand = new MarkCommand()
            .setName("mark")
            .setDescription("read the date from all files in the current or the given directory and mark them to improve performance")
            .setCommandFormat("mark [/optional/directory]");

    /* available options with name, description, etc. */
    Option replaceOption = new FlagOption()
            .setName("replace")
            .setDescription("replace files that already exist instead of skipping them");

    Option moveOption = new FlagOption()
            .setName("move")
            .setDescription("move files instead of copying them");

    Option fileExtensionsOption = new ValueOption()
            .allowAllValues(true)
            .acceptMultipleValues(true)
            .setName("fileExtensions")
            .setDescription("constrain allowed file extensions")
            .setOptionFormat("--fileExtensions=[jpg,jpeg,png,txt,...]");

    Option ignoreMarkOption = new FlagOption()
            .setName("ignoreMark")
            .setDescription("ignores the mark set on the file and read the date from metadata instead");

    /**
     * Default empty constructor.
     */
    public Configuration() {}

    /**
     * Return a map with all available commands. The keys are the command's names.
     * @return
     */
    public Map<String, Command> allCommands() {
        Map<String, Command> allCommands = new HashMap<>();
        allCommands.put(organizeCommand.getName(), organizeCommand);
        allCommands.put(initCommand.getName(), initCommand);
        allCommands.put(statusCommand.getName(), statusCommand);
        allCommands.put(helpCommand.getName(), helpCommand);
        allCommands.put(versionCommand.getName(), versionCommand);
        allCommands.put(setPropertyCommand.getName(), setPropertyCommand);
        allCommands.put(deleteRepository.getName(), deleteRepository);
        allCommands.put(checkCommand.getName(), checkCommand);
        allCommands.put(repairCommand.getName(), repairCommand);
        allCommands.put(markCommand.getName(), markCommand);
        return allCommands;
    }

    /**
     * Return a map with all available options. The keys are the option's names.
     * @return
     */
    public Map<String, Option> allOptions() {
        Map<String, Option> allOptions = new HashMap<>();
        allOptions.put(replaceOption.getName(), replaceOption);
        allOptions.put(moveOption.getName(), moveOption);
        allOptions.put(fileExtensionsOption.getName(), fileExtensionsOption);
        allOptions.put(ignoreMarkOption.getName(), ignoreMarkOption);
        return allOptions;
    }

    /**
     * All properties in the repository given in property file
     * @return
     */
    public Properties getProperties() {
        return FileTools.readProperties(PROPERTY_FILE_PATH_STRING+ File.separator+PROPERTY_FILE_NAME_STRING);
    }

    /**
     * Get a set with all names of the properties.
     * @return
     */
    public Set<String> getPropertyNames() {
        return new HashSet<>(propertyNames);
    }

    /**
     * Get a set with all the names of the modifiable properties.
     * @return
     */
    public Set<String> getModifiableProperties() {
        return new HashSet<>(modifiableProperties);
    }
}
