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

public abstract class Organizer implements Subject<Integer> {
    private List<Observer> obs = new ArrayList<>();
    private int count = 0;
    protected ICopy operation;
    protected FileGraphOperation fileGraphOperation;

    private Set<String> allowedFileExtensions = null;

    public Organizer(ICopy operation, String repoPath) {
        this.operation = operation;
        fileGraphOperation = new FileGraphOperation(FileGraphFactory.get(repoPath));
    }

    public abstract void copyAndOrganize(String source);

    public void incrementCounter() {
        count++;
    }

    public void allowFileExtension(String ext) {
        if(allowedFileExtensions == null) allowedFileExtensions = new HashSet<>();
        allowedFileExtensions.add(ext);
    }

    public boolean fileExtensionAllowed(String ext) {
        if(allowedFileExtensions == null) return true;
        else return allowedFileExtensions.contains(ext);
    }

    public int getCount() {
        return this.count;
    }

    public void register(Observer o) {
        obs.add(o);
    }

    public void unregister(Observer o) {
        obs.remove(o);
    }

    public void notifyObservers() {
        for(Observer o : obs) {
            o.update();
        }
    }

    public Integer getState() {
        return count;
    }
}
