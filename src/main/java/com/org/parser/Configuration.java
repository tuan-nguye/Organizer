package com.org.parser;

import com.org.parser.command.*;
import com.org.parser.option.FlagOption;
import com.org.parser.option.Option;
import com.org.parser.option.ValueOption;
import com.org.parser.command.*;
import com.org.parser.option.*;
import com.org.util.FileTools;

import java.io.File;
import java.util.*;

public class Configuration {
    /* file where properties are stored */
    public static String PROPERTY_FILE_NAME_STRING = ".organizer_config.txt";
    public static String PROPERTY_FILE_PATH_STRING = "";


    Set<String> propertyNames = new HashSet<>(Arrays.asList("folderSize"));

    Set<String> modifiableProperties = new HashSet<>(Arrays.asList("folderSize"));

    /* available commands */
    Command organizeCommand = new OrganizeFiles()
            .setName("organize")
            .setDescription("copy and organize all files according to their time stamp into the repository in the current working directory")
            .setCommandFormat("organize /path/to/source");
    Command initCommand = new InitializeRepository()
            .setName("init")
            .setDescription("initialize org.organizer destination directory, default folder size is 500")
            .setCommandFormat("init [folderSize]");
    Command statusCommand = new PrintStatus()
            .setName("status")
            .setDescription("print status information in console");

    Command helpCommand = new PrintHelp()
            .setName("help")
            .setDescription("print help text");

    Command versionCommand = new PrintVersion()
            .setName("version")
            .setDescription("org.organizer version 0.1");

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

    /* available options */
    Option skipOption = new FlagOption()
            .setName("skip")
            .setDescription("skip files that already exist instead of replacing them");

    Option moveOption = new FlagOption()
            .setName("move")
            .setDescription("move files instead of copying them");

    Option fileExtensionsOption = new ValueOption()
            .allowAllValues(true)
            .acceptMultipleValues(true)
            .setName("fileExtensions")
            .setDescription("constrain allowed file extensions")
            .setOptionFormat("--fileExtensions=[jpg,jpeg,png,txt,...]");

    public Configuration() {
    }

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
        return allCommands;
    }

    public Map<String, Option> allOptions() {
        Map<String, Option> allOptions = new HashMap<>();
        allOptions.put(skipOption.getName(), skipOption);
        allOptions.put(moveOption.getName(), moveOption);
        allOptions.put(fileExtensionsOption.getName(), fileExtensionsOption);
        return allOptions;
    }

    public Properties getProperties() {
        return FileTools.readProperties(PROPERTY_FILE_PATH_STRING+ File.separator+PROPERTY_FILE_NAME_STRING);
    }

    public Set<String> getPropertyNames() {
        return new HashSet<>(propertyNames);
    }

    public Set<String> getModifiableProperties() {
        return new HashSet<>(modifiableProperties);
    }
}
