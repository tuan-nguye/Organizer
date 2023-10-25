package com.org.organizer;

import com.org.organizer.copy.ICopy;
import com.org.util.time.DateExtractor;
import com.org.organizer.copy.Move;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.graph.FileGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;

public class ThresholdOrganizer extends Organizer {
    private int threshold;
    private ICopy move = new Move();

    public ThresholdOrganizer(ICopy op, int threshold, String repoPath) {
        super(op, repoPath);
        this.threshold = threshold;
    }

    @Override
    public void copyAndOrganize(String source) {
        dfs(new File(source));
    }


    private void dfs(File file) {
        if(file.isFile()) {
            if(!fileExtensionAllowed(FileTools.getFileExtension(file))) return;
            copyFile(file);
            incrementCounter();
            notifyObservers();
        } else {
            for(File child : file.listFiles()) {
                dfs(child);
            }
        }
    }

    protected boolean copyFile(File f) {
        FileGraph.Node node = fileGraphOperation.copyFile(operation, f);
        if(node == null) return false;
        reorganize(node);
        return true;
    }

    /**
     * assumes that all files in the folder are in the correct folder.
     * iterates through all files and moves them to a newly created
     * subdirectory, if the threshold is exceeded
     * @param node
     */
    public void reorganize(FileGraph.Node node) {
        fileGraphOperation.reorganize(node, threshold);
    }
}
