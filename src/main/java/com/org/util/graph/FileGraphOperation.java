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
import java.util.*;
import java.util.regex.Pattern;

public class FileGraphOperation {
    private FileGraph fileGraph;
    private String errorFolderPath;

    public FileGraphOperation(FileGraph fileGraph) {
        this.fileGraph = fileGraph;
        errorFolderPath = fileGraph.getRoot().path + File.separator + Configuration.ERROR_FOLDER_NAME;
        File errorFolder = new File(errorFolderPath);
        if(!errorFolder.exists()) {
            addFolder(fileGraph.getRoot(), Configuration.ERROR_FOLDER_NAME);
        }
    }

    // TODO update numFilesSubTree and sizeTotal
    public FileGraph.Node copyFile(ICopy op, File file) {
        if(!file.exists()) return null;
        if(file.isHidden() && file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) return null;
        LocalDateTime dateTime = DateExtractor.getDate(file);

        FileGraph.Node node = getDirectory(dateTime);
        String fileName = FileTools.chooseFileName(node.path, file.getName(), dateTime);

        Path from = file.toPath(), to = Path.of(node.path, fileName);

        if(from.equals(to)) return null;

        boolean duplicate = to.toFile().exists();
        try {
            op.execute(from, to);
            DateExtractor.markFile(to.toFile(), dateTime);
        } catch(IOException ioe) {
            return null;
        }

        if(!duplicate) {
            node.fileCount++;
            return node;
        } else {
            return null;
        }
    }

    private FileGraph.Node getDirectory(LocalDateTime dateTime) {
        FileGraph.Node node;
        if(dateTime == null) node = getNode(new File(errorFolderPath));
        else node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();
        return node;
    }

    public void reorganize(FileGraph.Node node, int threshold) {
        if(node.depth == 6 ||node.fileCount <= threshold || node.path.equals(errorFolderPath)) return;
        node.leaf = false;
        File directory = new File(node.path);
        ICopy moveReplace = new MoveReplace();

        for(File file : directory.listFiles(a -> a.isFile())) {
            FileGraph.Node destNode = copyFile(moveReplace, file);
            if(destNode == null && !file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) {
                System.err.println("error during reorganizing");
            }
        }

        node.fileCount = 0;
        for(FileGraph.Node child : node.children.values()) {
            reorganize(child, threshold);
        }
    }


    /**
     * reduce the structure if it shouldnt be split
     * if the number of files in its children is not above the threshold
     * then it shouldn't be split, so copy all files in the child folders
     * into itself then delete children
     * dfs post order
     */
    public void reduceStructure(int threshold) {
        reduceStructure(threshold, fileGraph.getRoot());
    }

    private void reduceStructure(int threshold, FileGraph.Node node) {
        if(node.path.equals(errorFolderPath)) return;

        if(node.leaf) {
            // nothing to do
        } else {
            int numFiles = 0;
            boolean allLeaves = true;

            Set<Map.Entry<String, FileGraph.Node>> entries = new HashSet<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                if(child.path.equals(errorFolderPath)) continue;
                reduceStructure(threshold, child);
                numFiles += child.fileCount;
                allLeaves &= child.leaf;
                if(child.leaf && child.fileCount == 0) {
                    String key = e.getKey();
                    node.children.remove(key);
                    File emptyChild = new File(child.path);
                    if(!emptyChild.delete()) throw new IllegalStateException("couldnt delete empty child: " + child);
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
                        Path from = f.toPath(), to = currDir.resolve(fileName);
                        boolean duplicate = to.toFile().exists();
                        try {
                            moveOp.execute(from, to);
                            DateExtractor.markFile(to.toFile(), ldt);
                            if(duplicate) numFiles--;
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

    public FileGraph.Node addFolder(FileGraph.Node node, String folderName) {
        if(node == null) return null;
        String path = node.path + File.separator + folderName;
        if(!new File(path).mkdir()) return null;
        FileGraph.Node newNode = new FileGraph.Node(path, node.depth+1);
        node.children.put(path, newNode);
        node.leaf = false;
        return newNode;
    }

    /**
     *
     * @param node
     * @return returns the path from root to given node in a list
     */
    public List<FileGraph.Node> getPathToNode(FileGraph.Node node) {
        String[] folders = node.path.substring(fileGraph.getRoot().path.length()+1).split(Pattern.quote(File.separator));
        List<FileGraph.Node> path = new ArrayList<>();
        FileGraph.Node temp = fileGraph.getRoot();
        StringBuilder key = new StringBuilder(temp.path);
        path.add(temp);
        for(int i = 0; i < folders.length; i++) {
            key.append(File.separator).append(folders[i]);
            temp = temp.children.get(key.toString());
            path.add(temp);
        }

        return path;
    }

    public FileGraph.Node getNode(File folder) {
        String fullPath = folder.getAbsolutePath();
        FileGraph.Node root = fileGraph.getRoot();
        if(!fullPath.startsWith(root.path)) return null;

        FileGraph.Node node = root;
        StringBuilder key = new StringBuilder(node.path);
        String[] folders = fullPath.substring(fileGraph.getRoot().path.length()+1).split(Pattern.quote(File.separator));
        for(int i = 0; i < folders.length; i++) {
            key.append(File.separator).append(folders[i]);
            node = node.children.get(key.toString());
            if(node == null) return null;
        }

        return node;
    }
}
