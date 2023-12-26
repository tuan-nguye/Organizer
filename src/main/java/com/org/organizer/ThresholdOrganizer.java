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


/**
 * This organizer implementation organizes files by date and time, in the order
 * year -> month -> day of month -> hour -> minute -> second. A threshold limits
 * all folders to a maximum size. If a folder exceeds the limit then all files are copied
 * into newly created subdirectories. Files are only stored in the leaf folders and not
 * in any inner folders.
 */
public class ThresholdOrganizer extends Organizer {
    // file count threshold for folders
    private int threshold;

    /**
     * ThresholdOrganizer constructor
     * @param op copy or move operation to use on the files
     * @param threshold maximum number of files in each folder
     * @param repoPath repository path for the organizer, destination path
     */
    public ThresholdOrganizer(ICopy op, int threshold, String repoPath) {
        super(op, repoPath);
        this.threshold = threshold;
    }

    /**
     * Iterate through all files in the source directory recursively. Check the file's
     * extension. If it's allowed then look at the file's datetime attribute and search
     * for the correct location to copy/move it to. After copying/moving update its state
     * and notify all observers of the current progress.
     * @param source source directory as string path
     */
    @Override
    public void copyAndOrganize(String source) {
        dfs(new File(source));
    }

    /**
     * This function iterates through file objects recursively and is the implementation
     * of the ThresholdOrganizer's copyAndOrganize function.
     * @param file
     */
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

    /**
     * Copy or move a file to it's correct location. If a folder exceeds its size threshold
     * then reorganize all files in that folder.
     * @param f
     * @return
     */
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
