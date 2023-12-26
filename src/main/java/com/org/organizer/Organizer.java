package com.org.organizer;

import com.org.observer.Subject;
import com.org.organizer.copy.ICopy;
import com.org.parser.Configuration;
import com.org.util.graph.FileGraphFactory;
import com.org.observer.Observer;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This abstract class offers abstract methods that should be implemented in its
 * subclasses for organizing files. Also it implements the Subject interface
 * to count how many files have been organized if an observer needs monitoring.
 * The destination, in which the files are organized, is set during object creation.
 */
public abstract class Organizer implements Subject<Integer> {
    // list of observers that are to be updated
    private List<Observer> obs = new ArrayList<>();
    // count how many files have been organized in the current operation
    private int count = 0;
    // template class of the copy/move operation
    protected ICopy operation;
    // filegraphoperation used for executing complex file operations on the filegraph
    protected FileGraphOperation fileGraphOperation;
    // stores all file extensions that are allowed, all others are ignored
    private Set<String> allowedFileExtensions = null;

    /**
     * Organizer class constructor.
     * @param operation copy/move operation to be used when organizing
     * @param repoPath organizer repository path, destination path of the organizer
     */
    public Organizer(ICopy operation, String repoPath) {
        this.operation = operation;
        fileGraphOperation = new FileGraphOperation(FileGraphFactory.get(repoPath));
    }

    /**
     * Copy and organizer all files in the 'source' directory and copy/move them into
     * the repository path as its destination. The method searches through all files
     * recursively and only allows files with valid file extensions.
     * @param source source directory as string path
     */
    public abstract void copyAndOrganize(String source);

    /**
     * Increment the counter. This function should be called after successfully organizing
     * a file.
     */
    public void incrementCounter() {
        count++;
    }

    /**
     * Add a file extension to the set of allowed extensions
     * @param ext file extension as string
     */
    public void allowFileExtension(String ext) {
        if(allowedFileExtensions == null) allowedFileExtensions = new HashSet<>();
        allowedFileExtensions.add(ext);
    }

    /**
     * This function returns whether a file extension is allowed or not. If no file extensions
     * have been set as allowed, then allow all files.
     * @param ext file extension as string
     * @return true if the extension is allowed, false if not
     */
    public boolean fileExtensionAllowed(String ext) {
        if(allowedFileExtensions == null) return true;
        else return allowedFileExtensions.contains(ext);
    }

    /**
     * get the current count of processed/organized files.
     * @return
     */
    public int getCount() {
        return this.count;
    }

    /**
     * register an observer
     * @param o observer object
     */
    public void register(Observer o) {
        obs.add(o);
    }

    /**
     * unregister an observer
     * @param o observer object
     */
    public void unregister(Observer o) {
        obs.remove(o);
    }

    /**
     * Notify all observers by calling .update() on all registered observers.
     */
    public void notifyObservers() {
        for(Observer o : obs) {
            o.update();
        }
    }

    /**
     * get the organizer's state by returning how many files have been processed
     * by its current copyAndOrganize() method.
     * @return
     */
    public Integer getState() {
        return count;
    }
}
