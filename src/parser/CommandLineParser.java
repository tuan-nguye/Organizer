package parser;

import parser.command.Command;

public class CommandLineParser {
    public CommandLineParser() {}

    public Command parse(String[] args) {
        return null;
    }
}

/*
java -jar name.jar [command] [--option1 --option2 ... --optionN]

init --size=500
copy /source /destination --skip

 */