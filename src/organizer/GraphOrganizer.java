package organizer;

import util.FileDirectoryGraph;
import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;

/*
file count: 14800, size: 8218,852865

default time: 104137,522600 ms, graph time: 158038,449500 ms
graph is 65,893789% faster than default

GraphOrganizer is slower than my first default implementation Sadeg
 */
public class GraphOrganizer extends Organizer {
    private FileDirectoryGraph graph;

    @Override
    public String copyAndOrganize(String source, String destination) {
        graph = new FileDirectoryGraph(destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source or destination is not a directory");

        count = 0;
        errors.setLength(0);
        dfs(root);

        return errors.toString();
    }

    private void dfs(File file) {
        if(file.isFile()) {
            copyFile(file);
            count++;
            return;
        }

        for(File child : file.listFiles()) {
            dfs(child);
        }
    }

    private boolean copyFile(File f) {
        Path path = getDirectory(new Date(f.lastModified()));

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

    private Path getDirectory(Date date) {
        calendar.setTime(date);
        String year = String.valueOf(calendar.get(calendar.YEAR));
        String month = FileTools.folderName(months[calendar.get(calendar.MONTH)], year);

        if(!graph.directoryExists(year, month)) graph.add(year, month);
        return graph.get(year, month).toPath();
    }
}
