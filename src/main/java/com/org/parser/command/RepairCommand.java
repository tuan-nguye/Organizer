package com.org.parser.command;

import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.consistency.Checker;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelError;
import com.org.util.consistency.ModelFixer;
import com.org.util.graph.FileGraph;
import com.org.view.ProgressBar;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Repair the repository from all errors that can be found when executing the repair
 * command.
 */
public class RepairCommand extends Command {
    // store found errors in a map
    Map<ModelError, List<FileGraph.Node>> errors;
    // sum of all errors
    int sumErrors = 0;

    /**
     * Validate the configuration by checking for a valid repo and that there are
     * errors in the current folder structure.
     * @param args arguments
     * @param config configuration object
     * @throws CommandException if the repo doesn't exist or there aren't any errors to be fixed
     */
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        // check if the repo is valid
        Checker.checkRepositoryFile(config.PROPERTY_FILE_PATH_STRING);

        // get the checker functionality
        ModelChecker checker = new ModelChecker(config);

        // set up progress bar for checking all folder because that could take some time
        // by counting the number of folders
        int numberOfFolders = FileTools.countFolders(new File(config.PROPERTY_FILE_PATH_STRING));
        // the error folder should not be checked
        if(new File(config.PROPERTY_FILE_PATH_STRING + File.separator + Configuration.ERROR_FOLDER_NAME).exists()) numberOfFolders--;
        // setup of the subject-observer relationship
        ProgressBar bar = new ProgressBar(20, numberOfFolders);
        bar.setSubject(checker);
        checker.register(bar);

        // check the repo for errors
        System.out.println("searching for errors...");
        checker.checkAll();
        System.out.println();
        // count the number of errors
        errors = checker.getErrors();
        for(List<FileGraph.Node> ers : errors.values()) {
            sumErrors += ers.size();
        }

        // if there are no errors, then there is nothing to be fixed
        if(sumErrors == 0) throw new CommandException("no errors detected");
    }

    /**
     * Go through all found errors and fix them by modifying the folder structure.
     * @param args arguments
     * @param config configuration object
     */
    @Override
    public void executeCommand(String[] args, Configuration config) {
        // create the fixer object that includes all repairing functionality
        ModelFixer fixer = new ModelFixer(config);
        System.out.println("number of errors: " + sumErrors);

        // set up progress bar, which shows how many errors still need fixing
        ProgressBar bar = new ProgressBar(20, sumErrors);
        bar.setSubject(fixer);
        fixer.register(bar);

        // execute the model fixer
        fixer.fixStructure(errors);
    }
}
