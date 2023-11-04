package com.org.parser.command;

import com.org.parser.CommandException;
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

public class RepairCommand extends Command {
    Map<ModelError, List<FileGraph.Node>> errors;
    int sumErrors = 0;

    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        Checker.checkRepositoryFile(config.PROPERTY_FILE_PATH_STRING);

        ModelChecker checker = new ModelChecker(config);

        // set up progress bar
        int numberOfFolders = FileTools.countFolders(new File(config.PROPERTY_FILE_PATH_STRING));
        if(new File(config.PROPERTY_FILE_PATH_STRING + File.separator + Configuration.ERROR_FOLDER_NAME).exists()) numberOfFolders--;
        ProgressBar bar = new ProgressBar(20, numberOfFolders);
        bar.setSubject(checker);
        checker.register(bar);

        // check the repo for errors
        System.out.println("searching for errors...");
        checker.checkAll();
        System.out.println();
        errors = checker.getErrors();
        for(List<FileGraph.Node> ers : errors.values()) {
            sumErrors += ers.size();
        }

        if(sumErrors == 0) throw new CommandException("no errors detected");
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        ModelFixer fixer = new ModelFixer(config);
        System.out.println("number of errors: " + sumErrors);

        // set up progress bar
        ProgressBar bar = new ProgressBar(20, sumErrors);
        bar.setSubject(fixer);
        fixer.register(bar);

        // execute the model fixer
        fixer.fixStructure(errors);
    }
}
