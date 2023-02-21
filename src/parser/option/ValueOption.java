package parser.option;

import parser.ParseException;

import java.util.ArrayList;
import java.util.List;

public class ValueOption extends Option {
    private List<String> acceptedValues = new ArrayList<>();
    private List<String> values = new ArrayList<>();
    private List<String> defaultValues = new ArrayList<>();
    private boolean allowAll = false;
    private boolean acceptsMultipleValues = false;

    public ValueOption allowAllValues(boolean allowAll) {
        this.allowAll = allowAll;
        return this;
    }

    public ValueOption acceptMultipleValues(boolean acceptMultiple) {
        this.acceptsMultipleValues = acceptMultiple;
        return this;
    }

    public ValueOption addAcceptedValue(String value) {
        if(value == null || value.isEmpty()) throw new IllegalArgumentException("invalid value for option");
        acceptedValues.add(value);
        enabled = true;
        return this;
    }

    public ValueOption defaultValue(String value) {
        if(value == null || value.isEmpty()) throw new IllegalArgumentException("invalid value for option");
        defaultValues.add(value);
        enabled = true;
        return this;
    }

    @Override
    public void parseArguments(String input) throws ParseException {
        int idxAssign = input.indexOf('=');
        if(idxAssign == -1) throw new IllegalArgumentException("invalid input for value option");
        String[] args = input.substring(idxAssign+1).split(",");

        for(int i = 0; i < args.length; i++) {
            if(!allowAll && !acceptedValues.contains(args[i])) {
                throw new ParseException(String.format("option '%s' doesn't allow value '%s'", name, args[i]));
            }
            if(i >= 1 && !acceptsMultipleValues) {
                System.err.println("too many arguments for " + name + ", the extraneous values will be ignored");
                break;
            }
            values.add(args[i]);
        }
    }

    public List<String> getValues() {
        return values.size() != 0 ? new ArrayList<>(values) : new ArrayList<>(defaultValues);
    }
}
