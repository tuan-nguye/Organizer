package com.org.parser.command;

import com.org.parser.CommandException;
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

public class CheckCommand extends Command {
    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(!Checker.validRepository(config.PROPERTY_FILE_PATH_STRING)) {
            throw new CommandException("can't check uninitialized repository");
        }
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
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

        // print results
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();
        int maxLength = 0;

        System.out.println();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            ModelError me = e.getKey();
            maxLength = Math.max(maxLength, me.toString().length());
            List<FileGraph.Node> errorList = e.getValue();
            if(!errorList.isEmpty()) {
                System.out.println(me.name()+":");
                for(FileGraph.Node folder : errorList) {
                    System.out.println(folder.path);
                }
            }
        }

        System.out.println();

        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            ModelError me = e.getKey();
            int errorCount = e.getValue().size();
            int meStrLen = me.toString().length();
            System.out.printf("%s%s: %d\n", me.toString(), " ".repeat(maxLength-meStrLen), errorCount);
        }
    }
}
