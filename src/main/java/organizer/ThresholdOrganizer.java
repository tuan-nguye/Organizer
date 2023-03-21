package organizer;

import organizer.copy.ICopy;
import parser.Configuration;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;

public class ThresholdOrganizer extends Organizer {
    private FileGraph fileGraph;

    public ThresholdOrganizer(ICopy op) {
        super(op);
    }

    @Override
    public String organize(String source, String destination) {
        fileGraph = new FileGraph(destination);
        dfs(new File(fileGraph.getRoot().path));
        return errors.toString();
    }

    private void dfs(File file) {
        if(file.isFile()) {
            copyFile(file);
            count++;
            notifyObservers();
            return;
        } else {
            for(File child : file.listFiles()) {
                dfs(child);
            }
        }
    }

    @Override
    protected Path getDirectory(LocalDateTime dateTime) {
        return Path.of(fileGraph.getNode(dateTime).path);
    }
}
