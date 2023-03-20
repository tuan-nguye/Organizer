package organizer;

import observer.Observer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class Organizer implements observer.Subject<Integer> {
    protected Calendar calendar;
    protected String[] months;
    private List<Observer> obs;
    protected int count;
    protected StringBuilder errors;

    public Organizer() {
        obs = new ArrayList<>();
        calendar = Calendar.getInstance();
        months = new String[] {"jan", "feb", "maerz", "apr", "mai", "juni", "juli",
                "aug", "sept", "okt", "nov", "dez"};
        errors = new StringBuilder();
    }

    public abstract String copyAndOrganize(String source, String destination);

    protected boolean copyFile(File f) {
        Path path = getDirectory(new Date(f.lastModified()));
        if(path == null) return false;

        try {
            Files.copy(f.toPath(), path.resolve(f.getName()), StandardCopyOption.COPY_ATTRIBUTES);
        } catch(FileAlreadyExistsException faee) {
            return true;
        } catch(IOException ioe) {
            errors.append("copy file error: ").append(ioe.getMessage()).append("\n");
            return false;
        }

        return true;
    }
    protected abstract Path getDirectory(Date date);

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
