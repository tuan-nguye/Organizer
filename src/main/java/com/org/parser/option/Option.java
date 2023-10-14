package com.org.parser.option;

import com.org.parser.ParseException;

public abstract class Option {
    public static final String PREFIX = "--";

    protected String name;
    protected String description;
    String optionFormat;

    protected boolean enabled = false;

    public Option() {}

    public abstract void parseArguments(String input) throws ParseException;

    public abstract Option defaultValue(String value);

    public Option setName(String name) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("illegal name for option");
        this.name = name;
        return this;
    }

    public Option setDescription(String descr) {
        if(descr == null || descr.isEmpty()) throw new IllegalArgumentException("illegal description value");
        this.description = descr;
        return this;
    }

    public Option setOptionFormat(String format) {
        if(format == null || format.isEmpty()) throw new IllegalArgumentException("illegal option format value");
        this.optionFormat = format;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOptionFormat() {
        return optionFormat;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
