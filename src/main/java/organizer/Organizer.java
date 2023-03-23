package organizer;

import observer.Observer;
import organizer.copy.ICopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public abstract class Organizer implements observer.Subject<Integer> {
    private List<Observer> obs = new ArrayList<>();
    protected int count = 0;
    protected ICopy operation;

    public Organizer(ICopy operation) {
        this.operation = operation;
    }

    public abstract void copyAndOrganize(String source, String destination);

    public void incrementCounter() {
        count++;
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
