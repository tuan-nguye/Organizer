package com.org.parser.command;

import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.*;
import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.parser.option.Option;
import com.org.parser.option.ValueOption;
import com.org.util.consistency.Checker;
import com.org.util.FileTools;
import com.org.util.time.DateExtractor;
import com.org.view.ProgressBar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrganizeFiles extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(args.length != 1) {
            throw new CommandException("invalid number of arguments, needs 1");
        } // check if source and destination exist, destination is valid repo

        Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        String source = args[0], destination = config.PROPERTY_FILE_PATH_STRING;

        Map<String, Option> optionMap = config.allOptions();

        ValueOption extOption = (ValueOption) optionMap.get("fileExtensions");
        final Set<String> extensions = new HashSet<>();

        FilenameFilter filter = null;
        if(extOption.isEnabled()) {
            extensions.addAll(extOption.getValues());
            filter = (dir, name) -> extensions.contains(FileTools.getFileExtension(name).toLowerCase());
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

        int fileCount = FileTools.countFiles(sourceDir, filter);
        System.out.printf("file count: %d, size: %.2f%s\n", fileCount, formattedSize, sizeUnit[unit]);
        ProgressBar bar = new ProgressBar(20, fileCount);

        // get the IO operation
        ICopy copyOperation;
        boolean move = optionMap.get("move").isEnabled();
        boolean replace = optionMap.get("replace").isEnabled();
        String strOp;

        if(move) {
            strOp = replace ? "moving and replacing" : "moving";
            if(replace) copyOperation = new MoveReplace();
            else copyOperation = new Move();
        } else {
            strOp = replace ? "copying and replacing" : "copying";
            if(replace) copyOperation = new CopyReplace();
            else copyOperation = new Copy();
        }

        // get the ignoreMark option
        boolean ignoreMark = optionMap.get("ignoreMark").isEnabled();
        DateExtractor.setIgnoreMark(ignoreMark);

        int folderSize = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        Organizer thresholdOrganizer = new ThresholdOrganizer(copyOperation, folderSize, destination);

        for(String ext : extensions) thresholdOrganizer.allowFileExtension(ext);

        bar.setSubject(thresholdOrganizer);
        thresholdOrganizer.register(bar);

        System.out.printf("%s files %s -> %s\n", strOp, source, destination);
        thresholdOrganizer.copyAndOrganize(source);
    }
}
