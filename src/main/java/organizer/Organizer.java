package organizer;

import observer.Observer;
import organizer.copy.ICopy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Organizer implements observer.Subject<Integer> {
    private List<Observer> obs = new ArrayList<>();
    private int count = 0;
    protected ICopy operation;

    private Set<String> allowedFileExtensions = null;

    public Organizer(ICopy operation) {
        this.operation = operation;
    }

    public abstract void copyAndOrganize(String source, String destination);

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
