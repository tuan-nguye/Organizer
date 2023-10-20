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
            if(file.getAbsolutePath().equals(errorFolderPath)) return;
            for(File child : file.listFiles()) {
                dfs(child);
            }
        }
    }

    protected boolean copyFile(File f) {
        if(!f.exists()) return false;
        if(f.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) return false;
        LocalDateTime dateTime = DateExtractor.getDate(f);

        FileGraph.Node node = getDirectory(dateTime);
        Path path = Path.of(node.path);

        if(path == null) return false;

        try {
            operation.execute(f.toPath(), path.resolve(f.getName()));
        } catch(IOException ioe) {
            System.out.println("warning: " + ioe + ": " + ioe.getMessage());
            return false;
        }

        node.fileCount++;
        reorganize(node);
        return true;
    }

    protected FileGraph.Node getDirectory(LocalDateTime dateTime) {
        FileGraph.Node node;
        if(dateTime == null) node = errorNode;
        else node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();
        return node;
    }

    /**
     * assumes that all files in the folder are in the correct folder.
     * iterates through all files and moves them to a newly created
     * subdirectory, if the threshold is exceeded
     * @param node
     */
    public void reorganize(FileGraph.Node node) {
        if(node.fileCount <= threshold || node == errorNode) return;
        node.leaf = false;
        File directory = new File(node.path);

        for(File file : directory.listFiles(a -> a.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            LocalDateTime ldt = DateExtractor.getDate(file);
            FileGraph.Node nextNode = getDirectory(ldt);
            Path path = Path.of(nextNode.path);

            try {
                move.execute(file.toPath(), path.resolve(file.getName()));
                nextNode.fileCount++;
            } catch(IOException ioe) {
                System.err.println("error reorganizing " + file.getName());
                ioe.printStackTrace();
            }
        }

        node.fileCount = 0;
        for(FileGraph.Node child : node.children.values()) {
            reorganize(child);
        }
    }
}
