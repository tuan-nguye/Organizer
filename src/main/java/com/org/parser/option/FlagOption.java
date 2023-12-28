package com.org.parser.option;

import com.org.parser.ParseException;

/**
 * Flag option class implementing options that are either enabled or not.
 * They don't take in any value. If any are given, they will be ignored.
 */
public class FlagOption extends Option {
    /**
     * This option is enabled when it appears and doesn't take in any arguments.
     * @param input
     * @throws ParseException no exceptions will be thrown
     */
    @Override
    public void parseArguments(String input) throws ParseException {
        enabled = true;
    }

    /**
     * Flag options can be enabled by default if 'true' is set as value.
     * @param value
     * @return this object
     */
    @Override
    public FlagOption defaultValue(String value) {
        if(value == null || !value.equals("true")) return this;
        enabled = true;
        return this;
    }
}
