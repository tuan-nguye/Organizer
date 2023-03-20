package organizer;

import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Date;

public class YearMonthOrganizer extends Organizer {
    private Path destination;

    @Override
    public String copyAndOrganize(String source, String destination) {
        System.out.printf("source = %s, dest = %s\n", source, destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source or destination is not a directory");

        count = 0;
        errors.setLength(0);
        this.destination = Path.of(destination);
        dfs(root);

        return errors.toString();
    }

    private void dfs(File file) {
        if(file == null) return;
        else if(file.isFile()) {
            copyFile(file);
            count++;
            notifyObservers();
            return;
        }

        for(File f : file.listFiles()) {
            dfs(f);
        }
    }

    protected Path getDirectory(Date date) {
        calendar.setTime(date);
        String year = String.valueOf(calendar.get(calendar.YEAR));
        String month = FileTools.folderName(months[calendar.get(calendar.MONTH)], year);
        Path path = destination.resolve(Path.of(year, month));

        if(!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch(IOException ioe) {
                errors.append("create directory error: ").append(ioe.getMessage()).append("\n");
                path = null;
            }
        }

        return path;
    }
}
