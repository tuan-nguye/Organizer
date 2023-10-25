package com.org.util.graph;

import com.org.organizer.copy.ICopy;
import com.org.organizer.copy.Move;
import com.org.organizer.copy.MoveReplace;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.time.DateExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileGraphOperation {
    private FileGraph fileGraph;
    private FileGraph.Node errorNode;

    public FileGraphOperation(FileGraph fileGraph) {
        this.fileGraph = fileGraph;
        String errorFolderPath = fileGraph.getRoot().path + File.separator + Configuration.ERROR_FOLDER_NAME;
        errorNode = fileGraph.getRoot();
        File errorFolder = new File(errorFolderPath);
        if(!errorFolder.exists()) {
            errorFolder.mkdir();
            fileGraph.update(fileGraph.getRoot());
        }
        errorNode = errorNode.children.get(errorFolderPath);
    }

    public FileGraph.Node copyFile(ICopy op, File file) {
        if(!file.exists()) return null;
        if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) return null;
        LocalDateTime dateTime = DateExtractor.getDate(file);

        FileGraph.Node node = getDirectory(dateTime);
        String fileName = FileTools.chooseFileName(node.path, file.getName(), dateTime);
        Path path = Path.of(node.path, fileName);

        boolean duplicate = path.toFile().exists();
        try {
            op.execute(file.toPath(), path);
            DateExtractor.markFile(path.toFile(), dateTime);
        } catch(IOException ioe) {
            return null;
        }

        if(!duplicate) node.fileCount++;
        return node;
    }

    private FileGraph.Node getDirectory(LocalDateTime dateTime) {
        FileGraph.Node node;
        if(dateTime == null) node = errorNode;
        else node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();
        return node;
    }

    public void reorganize(FileGraph.Node node, int threshold) {
        if(node.depth == 6 ||node.fileCount <= threshold || node == errorNode) return;
        node.leaf = false;
        File directory = new File(node.path);

        for(File file : directory.listFiles(a -> a.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            LocalDateTime ldt = DateExtractor.getDate(file);
            FileGraph.Node nextNode = getDirectory(ldt);
            Path path = Path.of(nextNode.path);

            try {
                ICopy move = new Move();
                move.execute(file.toPath(), path.resolve(file.getName()));
                nextNode.fileCount++;
            } catch(IOException ioe) {
                System.err.println("error reorganizing " + file.getName());
                ioe.printStackTrace();
            }
        }

        node.fileCount = 0;
        for(FileGraph.Node child : node.children.values()) {
            reorganize(child, threshold);
        }
    }

    public void reduceStructure(int threshold) {
        reduceStructure(threshold, fileGraph.getRoot());
    }

    private void reduceStructure(int threshold, FileGraph.Node node) {
        if(node == errorNode) return;

        if(node.leaf) {
            // nothing to do
        } else {
            int numFiles = 0;
            boolean allLeaves = true;

            Set<Map.Entry<String, FileGraph.Node>> entries = new HashSet<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                if(child == errorNode) continue;
                reduceStructure(threshold, child);
                numFiles += child.fileCount;
                allLeaves &= child.leaf;
                if(child.leaf && child.fileCount == 0) {
                    String key = e.getKey();
                    node.children.remove(key);
                    File emptyChild = new File(child.path);
                    if(!emptyChild.delete()) throw new IllegalStateException("couldnt delete empty child: " + child + ", " + errorNode);
                }
            }

            if(allLeaves && numFiles <= threshold) {
                ICopy moveOp = new MoveReplace();
                Path currDir = Path.of(node.path);
                for(FileGraph.Node child : node.children.values()) {
                    File childFolder = new File(child.path);
                    for(File f : childFolder.listFiles()) {
                        LocalDateTime ldt = DateExtractor.getDate(f);
                        String fileName = FileTools.chooseFileName(node.path, f.getName(), ldt);
                        Path destinationPath = currDir.resolve(fileName);
                        try {
                            moveOp.execute(f.toPath(), destinationPath);
                            DateExtractor.markFile(destinationPath.toFile(), ldt);
                        } catch(IOException ioe) {
                            System.err.println("error moving during restructuring: " + f.getAbsolutePath());
                            ioe.printStackTrace();
                        }
                    }
                    if(!childFolder.delete()) throw new IllegalStateException("cant delete folder after moving all files: " + child.path);

                }

                node.children.clear();
                node.fileCount = numFiles;
            }

            if(node.children.isEmpty()) node.leaf = true;
        }
    }
}
