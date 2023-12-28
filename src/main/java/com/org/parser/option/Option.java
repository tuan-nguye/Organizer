package com.org.parser.option;

import com.org.parser.ParseException;

/**
 * Abstract class for all command line options. Each option has a name, description, and
 * an option format. This class parses the string input to extract the option values. Specific
 * implementations are in the subclasses.
 */
public abstract class Option {
    // each option in indicated with a prefix in the command line
    public static final String PREFIX = "--";
    // option name
    protected String name;
    // option description
    protected String description;
    // string describing how the option needs to be formatted
    String optionFormat;
    // boolean value to show whether the option in enabled in the current configuration
    protected boolean enabled = false;

    /**
     * Default option constructor. Variables should be set through the setters.
     */
    public Option() {}

    /**
     * Parse the string input to extract the values given to the option.
     * @param input
     * @throws ParseException if the input is invalid
     */
    public abstract void parseArguments(String input) throws ParseException;

    /**
     * Set the option's default value. This will be active even if it wasn't
     * enabled, unless stated otherwise.
     * @param value
     * @return
     */
    public abstract Option defaultValue(String value);

    /**
     * Set the option's name. Cannot be empty or null.
     * @param name
     * @return
     */
    public Option setName(String name) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal name for option");
        this.name = name;
        return this;
    }

    /**
     * Set the option's description. Cannot be empty or null.
     * @param descr
     * @return
     */
    public Option setDescription(String descr) {
        if(descr == null || descr.isEmpty()) throw new IllegalArgumentException("illegal description value");
        this.description = descr;
        return this;
    }

    /**
     * Set the option's format. Cannot be empty or null.
     * @param format
     * @return
     */
    public Option setOptionFormat(String format) {
        if(format == null || format.isEmpty()) throw new IllegalArgumentException("illegal option format value");
        this.optionFormat = format;
        return this;
    }

    /**
     * Get the option's name.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the option's description.
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the option's format.
     * @return
     */
    public String getOptionFormat() {
        return optionFormat;
    }

    /**
     * Return whether this options is enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}
