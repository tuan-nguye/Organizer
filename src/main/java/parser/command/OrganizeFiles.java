package parser.command;

import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.*;
import parser.CommandException;
import parser.Configuration;
import parser.option.Option;
import parser.option.ValueOption;
import util.consistency.Checker;
import util.FileTools;
import view.ProgressBar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrganizeFiles extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 2) {
            throw new CommandException("invalid number of arguments, needs 2");
        } // check if source and destination exist, destination is valid repo

        if(!Checker.validRepository(Configuration.PROPERTY_FILE_PATH_STRING)) {
            throw new CommandException(Configuration.PROPERTY_FILE_PATH_STRING + " is not a repository");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        String source = args[0], destination = args[1];
        System.out.printf("copying files %s -> %s\n", source, destination);

        Map<String, Option> optionMap = config.allOptions();

        ValueOption extOption = (ValueOption) optionMap.get("fileExtensions");
        final Set<String> extensions = new HashSet<>();

        FilenameFilter filter = null;
        if(extOption.isEnabled()) {
            extensions.addAll(extOption.getValues());
            filter = (dir, name) -> extensions.contains(FileTools.getFileExtension(name));
        }

        File sourceDir = new File(source);
        long size = FileTools.size(sourceDir, filter);
        double formattedSize = size;
        String[] sizeUnit = new String[] {"B", "KB", "MB", "GB", "TB", "PB"};
        int unit = 0;
        while(formattedSize >= 1000) {
            unit++;
            formattedSize /= 1000;
        }

        int fileCount = FileTools.count(sourceDir, filter);
        System.out.printf("file count: %d, size: %.2f%s\n", fileCount, formattedSize, sizeUnit[unit]);
        ProgressBar bar = new ProgressBar(20, fileCount);

        ICopy copyOperation;
        boolean move = optionMap.get("move").isEnabled();
        boolean skip = optionMap.get("skip").isEnabled();
        if(move && skip) copyOperation = new Move();
        else if(move) copyOperation = new MoveReplace();
        else if(skip) copyOperation = new CopyReplace();
        else copyOperation = new Copy();
        int folderSize = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        Organizer thresholdOrganizer = new ThresholdOrganizer(copyOperation, folderSize);

        for(String ext : extensions) thresholdOrganizer.allowExtension(ext);

        bar.setSubject(thresholdOrganizer);
        thresholdOrganizer.register(bar);

        thresholdOrganizer.copyAndOrganize(source, destination);
    }
}
