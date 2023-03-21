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
import java.util.Date;
import java.util.List;

public abstract class Organizer implements observer.Subject<Integer> {
    private List<Observer> obs = new ArrayList<>();
    protected int count;
    protected StringBuilder errors = new StringBuilder();
    protected ICopy operation;

    public Organizer(ICopy operation) {
        this.operation = operation;
    }

    public abstract String organize(String source, String destination);

    protected boolean copyFile(File f) {
        Instant instant = Instant.ofEpochMilli(f.lastModified());
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        Path path = getDirectory(dateTime);
        if(path == null) return false;

        try {
            operation.copy(f.toPath(), path.resolve(f.getName()));
        } catch(IOException ioe) {
            errors.append("copy file error: ").append(ioe.getMessage()).append("\n");
            return false;
        }

        return true;
    }
    protected abstract Path getDirectory(LocalDateTime dateTime);

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
