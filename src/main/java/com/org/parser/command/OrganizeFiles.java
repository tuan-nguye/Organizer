package com.org.parser.command;

import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.*;
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

/**
 * Implements the organization of files by datetime. The current working directory should
 * be the repo itself and the directory with the source files must be given as argument.
 */
public class OrganizeFiles extends Command {
    /**
     * validate that valid source files were given and that the current working directory
     * is a valid repo.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if number of args is wrong, source dir/file doesn't exist, repo is invalid
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // check mandatory argument
        if(args.length != 1) {
            throw new CommandException("invalid number of arguments, needs 1");
        }

        // check that the source file exists
        File sourceDir = new File(args[0]);
        if(!sourceDir.exists()) {
            throw new CommandException("source file: " + args[0] + " don't exist");
        }

        // check if current directory is a valid repo
        Checker.checkRepository(config.PROPERTY_FILE_PATH_STRING);
    }

    /**
     * Organizer the files by iterating through all of them recursively and copying
     * or moving them into the repository to their correct location by sorting them
     * by datetime. A progressbar will be set up and prints the current progress.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // get the source from the argument array and the destination from the configuration object
        String source = args[0], destination = config.PROPERTY_FILE_PATH_STRING;

        // get the option configurations
        Map<String, Option> optionMap = config.allOptions();

        // get the allowed file extensions if any are given
        ValueOption extOption = (ValueOption) optionMap.get("fileExtensions");
        final Set<String> extensions = new HashSet<>();

        // build a filter for filtering out files with allowed file extensions
        FilenameFilter filter = null;
        if(extOption.isEnabled()) {
            extensions.addAll(extOption.getValues());
            filter = (dir, name) -> extensions.contains(FileTools.getFileExtension(name).toLowerCase());
        }

        // sum up the total space of files to be moved
        File sourceDir = new File(source);
        long size = FileTools.size(sourceDir, filter);
        // the size is given in bytes, so format as much as possible
        double formattedSize = size;
        String[] sizeUnit = new String[] {"B", "KB", "MB", "GB", "TB", "PB"};
        int unit = 0;
        while(formattedSize >= 1000) {
            unit++;
            formattedSize /= 1000;
        }

        // get the total number of files and set up the progress bar
        int fileCount = FileTools.countFiles(sourceDir, filter);
        System.out.printf("file count: %d, size: %.2f%s\n", fileCount, formattedSize, sizeUnit[unit]);
        ProgressBar bar = new ProgressBar(20, fileCount);

        // get the IO operation, to find out whether to move/copy
        ICopy copyOperation;
        boolean move = optionMap.get("move").isEnabled();
        boolean replace = optionMap.get("replace").isEnabled();
        String strOp;

        // get the correct copy operation object
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

        // get the maximum allowed folder size which is stored in the properties file
        int folderSize = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        Organizer thresholdOrganizer = new ThresholdOrganizer(copyOperation, folderSize, destination);

        // add all extensions that are allowed
        // the list can also be empty, which means every extension is allowed
        for(String ext : extensions) thresholdOrganizer.allowFileExtension(ext);

        // set up the subject - observer references to update the progress bar
        // whenever a file has been successfully processed by the organizer
        bar.setSubject(thresholdOrganizer);
        thresholdOrganizer.register(bar);

        // start the execution of the algorithm
        System.out.printf("%s files %s -> %s\n", strOp, source, destination);
        thresholdOrganizer.copyAndOrganize(source);
    }
}
