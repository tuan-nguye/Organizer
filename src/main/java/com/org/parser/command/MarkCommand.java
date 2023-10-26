package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.consistency.Checker;
import com.org.util.time.DateExtractor;
import com.org.util.time.MarkAllFiles;
import com.org.view.ProgressBar;

import java.io.File;

public class MarkCommand extends Command {
    private String path;

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

        File folder = new File(path);
        if(!folder.exists()) throw new CommandException("directory " + path + " doesn't exist");
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        DateExtractor.setIgnoreMark(true);

        int fileCount = FileTools.countFiles(new File(path));
        System.out.println("number of files: " + fileCount);
        ProgressBar bar = new ProgressBar(20, fileCount);
        MarkAllFiles marker = new MarkAllFiles(path);

        bar.setSubject(marker);
        marker.register(bar);

        System.out.printf("marking all files in the directory '%s'...\n", path);
        marker.execute();
    }
}
