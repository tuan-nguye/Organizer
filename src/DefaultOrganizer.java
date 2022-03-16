import observer.Observer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DefaultOrganizer implements Organizer {
    private Calendar calendar;
    private Path dest;
    private String[] months;
    private List<Observer> obs;
    private int count;

    public DefaultOrganizer() {
        obs = new ArrayList<>();
        calendar = Calendar.getInstance();
        months = new String[] {"jan", "feb", "maerz", "apr", "mai", "juni", "juli", "aug",
        "sept", "okt", "nov", "dez"};
    }

    @Override
    public void copyAndOrganize(String source, String destination) {
        System.out.printf("source = %s, dest = %s\n", source, destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source is not a directory");

        this.dest = Path.of(destination);
        count = 0;
        dfs(root);
    }

    private void dfs(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                dfs(f);
            } else {
                copyFile(f);
                count++;
            }
        }
    }

    private boolean copyFile(File f) {
        Date date = new Date(f.lastModified());
        calendar.setTime(date);
        String year = String.valueOf(calendar.get(calendar.YEAR));
        String month = months[calendar.get(calendar.MONTH)] + "_" + year;
        Path path = dest.resolve(Path.of(year, month));

        if(!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }

        try {
            Files.copy(f.toPath(), path.resolve(f.getName()), StandardCopyOption.COPY_ATTRIBUTES);
        } catch(FileAlreadyExistsException faee) {
            System.out.printf("file %s already exists\n", f.getName());
            return false;
        } catch(IOException ioe) {
            System.out.printf("failed to copy file: %s, from %s to %s\n", f.getName(), f.toPath().toString(), path.toString());
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    public int getCount() {
        return this.count;
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
        for(Observer o : obs) {
            o.update();
        }
    }

    @Override
    public Object getState() {
        return count;
    }
}
