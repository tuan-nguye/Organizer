import organizer.*;
import parser.CommandException;
import parser.CommandLineParser;
import parser.Configuration;
import parser.ParseException;
import parser.command.Command;
import util.FileTools;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class Application {
    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        Configuration.PROPERTY_FILE_PATH_STRING = System.getProperty("user.dir");
        Map<String, Command> commandMap = configuration.allCommands();

        CommandLineParser parser = new CommandLineParser();
        parser.addCommand(commandMap);
        parser.addOption(configuration.allOptions());
        String[] parsedArgs = null;

        // parse the arguments from the command line
        try {
            parsedArgs = parser.parse(args);
        } catch(ParseException pe) {
            System.err.println(pe.getMessage());
            System.exit(1);
        }


        // create command object
        Command command = commandMap.get(parsedArgs[0]);
        parsedArgs = Arrays.copyOfRange(parsedArgs, 1, parsedArgs.length);

        // execute the command
        try {
            command.execute(parsedArgs, configuration);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
            System.exit(1);
        }
    }
}
