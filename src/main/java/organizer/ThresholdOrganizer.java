package organizer;

import organizer.copy.ICopy;
import organizer.copy.Move;
import parser.Configuration;
import util.graph.FileGraph;
import util.time.DateIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ThresholdOrganizer extends Organizer {
    private FileGraph fileGraph;
    private int threshold;
    private ICopy move = new Move();

    public ThresholdOrganizer(ICopy op, int threshold) {
        super(op);
        this.threshold = threshold;
    }

    @Override
    public String copyAndOrganize(String source, String destination) {
        fileGraph = new FileGraph(destination);
        dfs(new File(source));
        return errors.toString();
    }

    private void dfs(File file) {
        if(file.isFile()) {
            copyFile(file);
            fileGraph.printFileStructure();
            incrementCounter();
            notifyObservers();
            return;
        } else {
            for(File child : file.listFiles()) {
                dfs(child);
            }
        }
    }
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

    protected Path getDirectory(LocalDateTime dateTime) {
        FileGraph.FileNode node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();

        if(node.fileCount >= threshold) {
            reorganize(node);
            return getDirectory(dateTime);
        }

        node.fileCount++;
        return Path.of(node.path);
    }

    private void reorganize(FileGraph.FileNode node) {
        File directory = new File(node.path);
        String folder = node.depth > 0 ? node.path.substring(node.path.lastIndexOf(File.separator)+1) : "";

        for(File file : directory.listFiles(a -> a.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            DateIterator it = new DateIterator(dateTime(file.lastModified()));

            for(int i = 0; i < node.depth; i++) it.next();

            File nextDirFile = new File(node.path, folder + '_' + it.next());
            if(!nextDirFile.exists()) nextDirFile.mkdir();

            try {
                move.copy(file.toPath(), nextDirFile.toPath().resolve(file.getName()));
            } catch(IOException ioe) {
                System.err.println("error reorganizing " + file.getName());
                ioe.printStackTrace();
            }
        }

        fileGraph.update(node);
    }

    private LocalDateTime dateTime(long ms) {
        Instant instant = Instant.ofEpochMilli(ms);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime;
    }
}
