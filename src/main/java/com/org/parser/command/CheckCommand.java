package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.consistency.Checker;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelError;
import com.org.util.graph.FileGraph;
import com.org.view.ProgressBar;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This command checks a repository and validates that its structure is valid. Folders
 * should be named correctly and all files should be in their correct folder according
 * to their datetime attribute. It sums up all errors and prints them into the console
 * and lists them with all incorrect locations.
 */
public class CheckCommand extends Command {
    /**
     * Validate that the repository exists.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if the repo doesn't exist
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        Checker.checkRepositoryFile(config.PROPERTY_FILE_PATH_STRING);
    }

    /**
     * Count the number of folders and check for any errors in naming, location, etc.
     * A list of all errors can be found in the package util.consistency.ModelError.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // create the checker object which has all validation functionality
        ModelChecker checker = new ModelChecker(config);
        // set up the progress bar
        // it shows the number of folders that have been checked
        int numberOfFolders = FileTools.countFolders(new File(config.PROPERTY_FILE_PATH_STRING), File::isDirectory);
        if(new File(config.PROPERTY_FILE_PATH_STRING + File.separator + Configuration.ERROR_FOLDER_NAME).exists()) numberOfFolders--;
        System.out.println("number of folders: " + numberOfFolders);
        ProgressBar bar = new ProgressBar(20, numberOfFolders);
        bar.setSubject(checker);
        checker.register(bar);

        // execute the check
        checker.checkAll();

        // get all errors and their folders
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();
        int maxLength = 0;

        // print all errors and all folders which have this error
        System.out.println();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            ModelError me = e.getKey();
            // get the max string length for padding reasons for later
            maxLength = Math.max(maxLength, me.toString().length());
            List<FileGraph.Node> errorList = e.getValue();
            if(!errorList.isEmpty()) {
                System.out.println(me.name()+":");
                for(FileGraph.Node folder : errorList) {
                    if(folder != null) System.out.println(folder.path);
                }
            }
        }

        // print a summary table showing all errors and how often they occurred
        System.out.println();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            ModelError me = e.getKey();
            int errorCount = e.getValue().size();
            int meStrLen = me.toString().length();
            System.out.printf("%s%s: %d\n", me.toString(), " ".repeat(maxLength-meStrLen), errorCount);
        }
    }
}
