import organizer.*;
import parser.CommandException;
import parser.CommandLineParser;
import parser.Configuration;
import parser.ParseException;
import parser.command.Command;
import parser.command.PrintHelp;
import util.FileTools;
import view.ProgressBar;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class Application {
    public static void main(String[] args) {
        /*
        File source = new File(args[0]);
        int fileCount = FileTools.count(source);
        long size = FileTools.size(source);
        System.out.printf("file count: %d, size: %f\n", fileCount, size/1e6);
        FileTools.clearToTrashbin(new File(args[1]));

        ProgressBar bar = new ProgressBar(20, fileCount);
        Organizer org = new ThresholdOrganizer(5);

        bar.setSubject(org);
        org.register(bar);

        try {
            String errors = org.copyAndOrganize(args[0], args[1]);
            System.out.println(errors);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        */

        Configuration configuration = new Configuration();
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

    private static void defaultVsGraph(String source, String dest) {
        FileTools.clearToTrashbin(new File(dest + "/test1"));
        FileTools.clearToTrashbin(new File(dest + "/test2"));

        Organizer dOrg = new YearMonthOrganizer(), gOrg = new YearMonthGraphOrganizer();

        long start = System.nanoTime(), def = 0, graph = 0;
        try {
            String errors = dOrg.copyAndOrganize(source, dest + "/test1");
            def = System.nanoTime() - start;
            System.out.println(errors);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        start = System.nanoTime();
        try {
            String errors = gOrg.copyAndOrganize(source, dest + "/test2");
            graph = System.nanoTime() - start;
            System.out.println(errors);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.printf("default time: %f, graph time: %f\n", def / 1e6, graph / 1e6);
        System.out.printf("graph is %f%% faster than default\n", 100 * (def / (double) graph));
    }
}
