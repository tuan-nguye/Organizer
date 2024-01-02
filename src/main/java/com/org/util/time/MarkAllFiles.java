package com.org.util.time;

import com.org.observer.Observer;
import com.org.observer.Subject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements functionality to mark files to increase performance by
 * reducing the amount of IO operations needed to read the datetime attribute.
 * Details to how the file is marked are in the DateExtractor class. The execution
 * features registering observers to track the current progress.
 */
public class MarkAllFiles implements Subject<Integer> {
    // absolute path as string to the repository
    private String repoPath;
    // list of observers for the subject-observer pattern
    private List<Observer> obs = new ArrayList<>();
    // number of currently marked files after execution, subject-state
    int markedFilesCount = 0;

    /**
     * MarkAllFiles constructor
     * @param repoPath absolute path to the repository as string
     */
    public MarkAllFiles(String repoPath) {
        this.repoPath = repoPath;
    }

    /**
     * Marks all the files by recursively going through the filesystem structure.
     * The root is the repository path given when creating the object.
     */
    public void execute() {
        // reset the state
        markedFilesCount = 0;
        execute(new File(repoPath));
    }

    /**
     * Recursive implementation of marking all files.
     * @param file file object
     */
    private void execute(File file) {
        if(file == null || !file.exists()) return;
        if(file.isFile()) {
            DateExtractor.markFile(file);
            markedFilesCount++;
            notifyObservers();
        } else {
            for(File f : file.listFiles()) {
                execute(f);
            }
        }
    }

    /**
     * Register a new observer.
     * @param o
     */
    @Override
    public void register(Observer o) {
        obs.add(o);
    }

    /**
     * Unregister an observer
     * @param o
     */
    @Override
    public void unregister(Observer o) {
        obs.remove(o);
    }

    /**
     * Notify all observers.
     */
    @Override
    public void notifyObservers() {
        for(Observer o : obs) o.update();
    }

    /**
     * Get the object's state.
     * @return
     */
    @Override
    public Integer getState() {
        return markedFilesCount;
    }
}
