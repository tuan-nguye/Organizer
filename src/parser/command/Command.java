package parser.command;

import parser.option.Option;

import java.util.List;
import java.util.Properties;

public interface Command {
    void execute(Properties properties, List<Option> options);
}
