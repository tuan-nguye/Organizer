package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.time.DateExtractor;
import com.org.util.time.MarkAllFiles;
import com.org.view.ProgressBar;

import java.io.File;

/**
 * This command iterates through all files reads the datetime attribute from them
 * and marks them by changing the ms (the last 3 digits) of the file.lastModified()
 * file attribute. The mark is a hash from the file's name. The next time the file's
 * datetime is read, the mark is checked. If the file is marked, then accessing the
 * lastModified field saves a lot of IO operations and increases the other file operations
 * significantly.
 */
public class MarkCommand extends Command {
    // the directory's path to be marked
    private String path;

    /**
     * This command doesn't need to be executed on the repo. It takes in an optional argument
     * to indicate the directory it should be run on. If none was given it takes the current working
     * directory. The method will not work if the given folder doesn't exist.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if more than 1 argument was given or the folder doesn't exist
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // do nothing, marking a directory that isn't a repository is also ok
        if(args.length > 1) {
            throw new CommandException("too many arguments, only 0 or 1 are allowed");
        } else if(args.length == 1) {
            path = args[0];
        } else {
            path = config.PROPERTY_FILE_PATH_STRING;
        }

        // check if the folder exists
        File folder = new File(path);
        if(!folder.exists()) throw new CommandException("directory " + path + " doesn't exist");
    }

    /**
     * Execute the command and mark all files in the given directory. This method
     * also searches through all the subdirectories.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // deactivate the mark function when reading a file's datetime
        DateExtractor.setIgnoreMark(true);

        // setup the progress bar by counting the number of files
        int fileCount = FileTools.countFiles(new File(path));
        System.out.println("number of files: " + fileCount);
        ProgressBar bar = new ProgressBar(20, fileCount);
        // create the marker object
        MarkAllFiles marker = new MarkAllFiles(path);

        // register the progress bar and setup the marker as subject
        bar.setSubject(marker);
        marker.register(bar);

        System.out.printf("marking all files in the directory '%s'...\n", path);
        marker.execute();
    }
}
