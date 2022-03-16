package organizer;

import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Date;

public class DefaultOrganizer extends Organizer {
    @Override
    public String copyAndOrganize(String source, String destination) {
        System.out.printf("source = %s, dest = %s\n", source, destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source is not a directory");

        count = 0;
        errors.setLength(0);
        dfs(root, Path.of(destination));

        return errors.toString();
    }

    private void dfs(File file, Path dest) {
        if(file == null) return;
        else if(file.isFile()) {
            copyFile(file, dest);
            count++;
            return;
        }

        for(File f : file.listFiles()) {
            dfs(f, dest);
        }
    }

    private boolean copyFile(File f, Path dest) {
        Path path = getDirectory(new Date(f.lastModified()), dest);
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

    private Path getDirectory(Date date, Path dest) {
        calendar.setTime(date);
        String year = String.valueOf(calendar.get(calendar.YEAR));
        String month = months[calendar.get(calendar.MONTH)] + "_" + year;
        Path path = dest.resolve(Path.of(year, month));

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
