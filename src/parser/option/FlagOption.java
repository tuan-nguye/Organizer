package parser.option;

public class FlagOption extends Option {
    @Override
    public void parseArguments(String input) {
        enabled = true;
    }

    @Override
    public FlagOption defaultValue(String value) {
        if(value == null || !value.equals("true")) return this;
        enabled = true;
        return this;
    }
}
