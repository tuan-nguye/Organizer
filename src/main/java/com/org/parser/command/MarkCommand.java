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
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        DateExtractor.setIgnoreMark(true);
        String repoPath = config.PROPERTY_FILE_PATH_STRING;

        int fileCount = FileTools.countFiles(new File(repoPath));
        System.out.println("number of files: " + fileCount);
        ProgressBar bar = new ProgressBar(20, fileCount);
        MarkAllFiles marker = new MarkAllFiles(repoPath);

        bar.setSubject(marker);
        marker.register(bar);

        System.out.printf("marking all files in the directory %s...", repoPath);
        marker.execute();
    }
}
