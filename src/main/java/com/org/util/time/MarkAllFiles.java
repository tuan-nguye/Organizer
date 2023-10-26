package com.org.util.time;

import com.org.observer.Observer;
import com.org.observer.Subject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MarkAllFiles implements Subject<Integer> {
    private String repoPath;
    private List<Observer> obs = new ArrayList<>();
    int markedFilesCount = 0;

    public MarkAllFiles(String repoPath) {
        this.repoPath = repoPath;
    }

    public void execute() {
        execute(new File(repoPath));
    }

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

    @Override
    public void register(Observer o) {
        obs.add(o);
    }

    @Override
    public void unregister(Observer o) {
        obs.remove(o);
    }

    @Override
    public void notifyObservers() {
        for(Observer o : obs) o.update();
    }

    @Override
    public Integer getState() {
        return markedFilesCount;
    }
}
