package organizer;

import organizer.copy.ICopy;
import organizer.copy.Move;
import parser.Configuration;
import util.FileTools;
import util.graph.FileGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ThresholdOrganizer extends Organizer {
    private FileGraph fileGraph;
    private int threshold;
    private ICopy move = new Move();

    public ThresholdOrganizer(ICopy op, int threshold) {
        super(op);
        this.threshold = threshold;
    }

    @Override
    public void copyAndOrganize(String source, String destination) {
        fileGraph = new FileGraph(destination);
        dfs(new File(source));
    }

    private void dfs(File file) {
        if(file.isFile()) {
            copyFile(file);
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
        LocalDateTime dateTime = FileTools.dateTime(f.lastModified());
        FileGraph.FileNode node = getDirectory(dateTime);
        Path path = Path.of(node.path);
        if(path == null) return false;

        try {
            operation.execute(f.toPath(), path.resolve(f.getName()));
        } catch(IOException ioe) {
            System.out.println("warning: " + ioe.getMessage());
            return false;
        }

        node.fileCount++;
        reorganize(node);
        return true;
    }

    protected FileGraph.FileNode getDirectory(LocalDateTime dateTime) {
        FileGraph.FileNode node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();
        return node;
    }

    private void reorganize(FileGraph.FileNode node) {
        if(node.fileCount <= threshold) return;
        node.leaf = false;
        File directory = new File(node.path);

        for(File file : directory.listFiles(a -> a.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            FileGraph.FileNode nextNode = getDirectory(FileTools.dateTime(file.lastModified()));
            Path path = Path.of(nextNode.path);

            try {
                move.execute(file.toPath(), path.resolve(file.getName()));
            } catch(IOException ioe) {
                System.err.println("error reorganizing " + file.getName());
                ioe.printStackTrace();
            }
        }

        fileGraph.update(node);
        for(FileGraph.FileNode child : node.children.values()) {
            reorganize(child);
        }
    }
}
