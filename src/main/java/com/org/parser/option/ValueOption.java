package com.org.parser.option;

import com.org.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Value options require at least one value and can have multiple values.
 */
public class ValueOption extends Option {
    // list of accepted values
    private List<String> acceptedValues = new ArrayList<>();
    // list of parsed values
    private List<String> values = new ArrayList<>();
    // list of default values
    private List<String> defaultValues = new ArrayList<>();
    // allow all possible values as input
    private boolean allowAll = false;
    // allow multiple values
    private boolean acceptsMultipleValues = false;

    /**
     * Sets all values as allowed.
     * @param allowAll
     * @return
     */
    public ValueOption allowAllValues(boolean allowAll) {
        this.allowAll = allowAll;
        return this;
    }

    /**
     * Sets that this option can take in multiple values.
     * @param acceptMultiple
     * @return
     */
    public ValueOption acceptMultipleValues(boolean acceptMultiple) {
        this.acceptsMultipleValues = acceptMultiple;
        return this;
    }

    /**
     * Add a value that is allowed for the option.
     * @param value
     * @return
     */
    public ValueOption addAcceptedValue(String value) {
        if(value == null || value.isEmpty()) throw new IllegalArgumentException("invalid value for option");
        acceptedValues.add(value);
        enabled = true;
        return this;
    }

    /**
     * Add a default values to this option. The option will be enabled with the given
     * values.
     * @param value
     * @return
     */
    public ValueOption defaultValue(String value) {
        if(value == null || value.isEmpty()) throw new IllegalArgumentException("invalid value for option");
        defaultValues.add(value);
        enabled = true;
        return this;
    }

    /**
     * Parse the arguments by splitting the name and the values between the '=' sign.
     * Then split the values string between commas because they should be formatted
     * comma-separated. All given values are added to the list of enabled values.
     * @param input string input from command line
     * @throws ParseException
     */
    @Override
    public void parseArguments(String input) throws ParseException {
        // check that values are given after the equals sign
        int idxAssign = input.indexOf('=');
        if(idxAssign == -1) throw new ParseException("invalid input for value option");
        // split up the values and check that it's not empty
        input = input.substring(idxAssign+1);
        if(input.isEmpty()) throw new ParseException("no values given for value option");
        // split the values with a comma as delimiter
        String[] args = input.split(",");

        // iterate through all values and add them to the enabled list
        // if they are accepted or all values are allowed
        for(int i = 0; i < args.length; i++) {
            if(!allowAll && !acceptedValues.contains(args[i])) {
                throw new ParseException(String.format("option '%s' doesn't allow value '%s'", name, args[i]));
            }
            // multiple values can only be accepted if the option allows it
            if(i >= 1 && !acceptsMultipleValues) {
                System.err.println("too many arguments for " + name + ", the extraneous values will be ignored");
                break;
            }
            // add value to the list of enabled values
            values.add(args[i]);
        }

        // set the option as enabled
        enabled = true;
    }

    /**
     * Return the option's values as a list of strings.
     * @return list with all enabled values. the list can be empty if the options is disabled
     */
    public List<String> getValues() {
        return values.size() != 0 ? new ArrayList<>(values) : new ArrayList<>(defaultValues);
    }
}
