package com.org.parser.command;

import com.org.parser.Configuration;

/**
 * abstract class that implements the command pattern. Each command has a name, a description,
 * and a command format. Commands have a validation method ensuring that the correct arguments
 * are present.
 */
public abstract class Command {
    // command name
    private String name;
    // command description, describes its function
    private String description;
    // describes the commands format and how to use it in the command line
    private String commandFormat;

    /**
     * Default empty constructor
     */
    public Command() {}

    /**
     * execute the command after validating its input. Implementation in the subclasses
     * as template method.
     * @param args arguments given as string array
     * @param config configuration object
     * @throws CommandException
     */
    public void execute(String[] args, Configuration config) throws CommandException {
        validateConfiguration(args, config);
        executeCommand(args, config);
    }

    /**
     * Validate the current configuration and the given arguments. If anything is invalid
     * then throw an exception
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if either input is invalid or the configuration is incorrect
     */
    public abstract void validateConfiguration(String[] args, Configuration config) throws CommandException;

    /**
     * execute the command. This function contains the actual implementation.
     * @param args arguments
     * @param config configuration object
     */
    public abstract void executeCommand(String[] args, Configuration config);

    /**
     * setter for the command format
     * @param commandFormat input as string
     * @return this object
     */
    public Command setCommandFormat(String commandFormat) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal command name");
        this.commandFormat = commandFormat;
        return this;
    }

    /**
     * setter for the command's name
     * @param name string name
     * @return this object
     */
    public Command setName(String name) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal command name");
        this.name = name;
        return this;
    }

    /**
     * setter for the command's description
     * @param descr description as string
     * @return this object
     */
    public Command setDescription(String descr) {
        if(descr == null || descr.isEmpty()) throw new IllegalArgumentException("illegal description value");
        this.description = descr;
        return this;
    }

    /**
     * getter for the object's name
     * @return command name
     */
    public String getName() {
        return name;
    }

    /**
     * getter for the object's description
     * @return command description
     */
    public String getDescription() {
        return description;
    }

    /**
     * getter for the command's format
     * @return command format
     */
    public String getCommandFormat() {
        return commandFormat;
    }
}
