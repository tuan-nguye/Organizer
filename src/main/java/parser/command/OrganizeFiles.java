package parser.command;

import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.*;
import parser.CommandException;
import parser.Configuration;
import parser.option.Option;
import util.FileTools;
import view.ProgressBar;

import java.io.File;
import java.util.Map;

public class OrganizeFiles extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 2) {
            throw new CommandException("invalid number of arguments, needs 2");
        } // check if source and destination exist, destination is valid repo

    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        System.out.println("source=" + args[0]);
        System.out.println("destination=" + args[1]);

        String source = args[0], destination = args[1];
        File sourceDir = new File(source);
        long size = FileTools.size(sourceDir);
        double formattedSize = size;
        String[] sizeUnit = new String[] {"B", "KB", "MB", "GB", "TB", "PB"};
        int unit = 0;
        while(formattedSize >= 1000) {
            unit++;
            formattedSize /= 1000;
        }

        int fileCount = FileTools.count(sourceDir);
        System.out.printf("file count: %d, size: %.2f%s\n", fileCount, formattedSize, sizeUnit[unit]);
        ProgressBar bar = new ProgressBar(20, fileCount);

        ICopy copyOperation;
        Map<String, Option> optionMap = config.allOptions();
        boolean move = optionMap.get("move").isEnabled();
        boolean skip = optionMap.get("skip").isEnabled();
        if(move && skip) copyOperation = new Move();
        else if(move) copyOperation = new MoveReplace();
        else if(skip) copyOperation = new CopyReplace();
        else copyOperation = new Copy();
        int folderSize = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        Organizer thresholdOrganizer = new ThresholdOrganizer(copyOperation, folderSize);

        bar.setSubject(thresholdOrganizer);
        thresholdOrganizer.register(bar);

        thresholdOrganizer.copyAndOrganize(source, destination);
    }
}
