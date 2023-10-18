package com.org.parser.command;

import com.org.parser.CommandException;
import com.org.parser.Configuration;
import com.org.util.consistency.Checker;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelError;
import com.org.util.consistency.ModelFixer;
import com.org.util.graph.FileGraph;

import java.util.List;
import java.util.Map;

public class RepairCommand extends Command {
    Map<ModelError, List<FileGraph.Node>> errors;

    @Override
    public void validateConfiguration(String[] args, Configuration config) throws CommandException {
        if(!Checker.validRepository(config.PROPERTY_FILE_PATH_STRING)) {
            throw new CommandException("can't repair uninitialized repository");
        }

        ModelChecker checker = new ModelChecker(config);
        checker.checkAll();
        errors = checker.getErrors();
        int sum = 0;
        for(List<FileGraph.Node> ers : errors.values()) {
            sum += ers.size();
        }

        if(sum == 0) throw new CommandException("no errors detected");
    }

    @Override
    public void executeCommand(String[] args, Configuration config) {
        ModelFixer fixer = new ModelFixer(config);
        fixer.fixStructure(errors);
    }
}
