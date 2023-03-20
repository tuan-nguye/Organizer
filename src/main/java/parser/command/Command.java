package parser.command;

import parser.CommandException;
import parser.Configuration;

public abstract class Command {
    private String name;
    private String description;
    private String commandFormat;

    public Command() {}

    public void execute(String[] args, Configuration config) throws CommandException {
        validateConfiguration(args, config);
        executeCommand(args, config);
    }

    public abstract void validateConfiguration(String[] args, Configuration config) throws CommandException;

    public abstract void executeCommand(String[] args, Configuration config);

    public Command setCommandFormat(String commandFormat) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal command name");
        this.commandFormat = commandFormat;
        return this;
    }

    public Command setName(String name) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal command name");
        this.name = name;
        return this;
    }

    public Command setDescription(String descr) {
        if(descr == null || descr.isEmpty()) throw new IllegalArgumentException("illegal description value");
        this.description = descr;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCommandFormat() {
        return commandFormat;
    }
}
