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

/**
 * File graphs have an internal state that needs to be updated every time the filesystem
 * is changed. This class offers functions for executing complex function working
 * on the file graph and updating the file graph accordingly.
 */
public class FileGraphOperation {
    // the file graph to use the operations on
    private FileGraph fileGraph;
    // string path to the error folder
    private String errorFolderPath;

    /**
     * FileGraphOperation constructor. The constructor will create the error folder
     * if it doesn't exist.
     * @param fileGraph
     */
    public FileGraphOperation(FileGraph fileGraph) {
        this.fileGraph = fileGraph;
        errorFolderPath = fileGraph.getRoot().path + File.separator + Configuration.ERROR_FOLDER_NAME;
        File errorFolder = new File(errorFolderPath);
        if(!errorFolder.exists()) {
            addFolder(fileGraph.getRoot(), Configuration.ERROR_FOLDER_NAME);
        }
    }

    // TODO update numFilesSubTree and sizeTotal
    /**
     * Copy a file into the file graph using the file graph's datetime functionality.
     * This function makes sure to create the folder if it didn't exist and also updates
     * the node's fields.
     * @param op file operation, copy/move
     * @param file file that needs to be copied/moved
     * @return
     */
    public FileGraph.Node copyFile(ICopy op, File file) {
        if(!file.exists()) return null;
        // skipt the hidden config file
        if(file.isHidden() && file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) return null;
        // get the correct node and folder to save it to
        LocalDateTime dateTime = DateExtractor.getDate(file);
        FileGraph.Node node = getDirectory(dateTime);
        // duplicates can exist, so choose either a new name or ignore/replace the file
        String fileName = FileTools.chooseFileName(node.path, file.getName(), dateTime);
        // if source and destination are the same, don't need to do anything
        Path from = file.toPath(), to = Path.of(node.path, fileName);
        if(from.equals(to)) return null;

        boolean duplicate = to.toFile().exists();
        try {
            op.execute(from, to);
            // mark the file for increased performance in subsequent datetime accesses
            DateExtractor.markFile(to.toFile(), dateTime);
        } catch(IOException ioe) {
            return null;
        }
        // if the file was a duplicate then it was ignored or replaced, the file count doesn't change
        if(!duplicate) {
            updateNode(node, file);
            return node;
        } else {
            return null;
        }
    }

    /**
     * This function updates the node's fields after the file was moved into its directory.
     * The node's file count and size will change according to the file's size in bytes.
     * @param node
     * @param file
     */
    private void updateNode(FileGraph.Node node, File file) {
        node.fileCount++;
        node.sizeTotal += file.length();
    }

    /**
     * Gets the correct directory for a file using the datetime stamp. The getNode() function
     * returns a node, where the folder might not exist. So this function gets the correct
     * node and creates the folder.
     * @param dateTime date and time
     * @return the node/folder of the file graph for the datetime
     */
    private FileGraph.Node getDirectory(LocalDateTime dateTime) {
        FileGraph.Node node;
        if(dateTime == null) node = getNode(new File(errorFolderPath));
        else node = fileGraph.getNode(dateTime);
        new File(node.path).mkdir();
        return node;
    }

    /**
     * Reorganize a folder by creating new subfolders and moving all files in the current
     * folder into the newly created children folders. This function takes in the folder
     * size threshold. If the threshold is not exceeded it won't do anything.
     * @param node the node/folder to reorganize
     * @param threshold folder size threshold
     */
    public void reorganize(FileGraph.Node node, int threshold) {
        // can't reorganize if the folder depth is too high (beyond seconds), the threshold
        // is not exceeded. The error folder should never be reorganized
        if(node.depth == 6 || node.fileCount <= threshold || node.path.equals(errorFolderPath)) return;
        // marking the node as an inner node makes sure that the files are moved to
        // new child folder that is created in the current folder
        node.leaf = false;
        File directory = new File(node.path);
        ICopy moveReplace = new MoveReplace();

        for(File file : directory.listFiles(a -> a.isFile())) {
            FileGraph.Node destNode = copyFile(moveReplace, file);
            if(destNode == null && !file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) {
                System.err.println("error during reorganizing");
            }
        }

        // reorganize all the child nodes afterwards, because they could still be
        // above the threshold
        node.fileCount = 0;
        for(FileGraph.Node child : node.children.values()) {
            reorganize(child, threshold);
        }
    }


    /**
     * reduce the structure if it shouldn't be split or folders are empty
     * if the number of files in its children is not above the threshold
     * then it shouldn't be split, so copy all files in the child folders
     * into itself then delete children
     * @param threshold folder size threshold
     */
    public void reduceStructure(int threshold) {
        reduceStructure(threshold, fileGraph.getRoot());
    }

    /**
     * This function implements the recursive reduceStructure() functionality. It's implemented
     * as a dfs post-order algorithm. It will reduce the lower nodes first. Afterwards, the parent
     * nodes can be reduced.
     * @param threshold
     * @param node
     */
    private void reduceStructure(int threshold, FileGraph.Node node) {
        // skip the error folder
        if(node.path.equals(errorFolderPath)) return;

        if(node.leaf) {
            // nothing to do
        } else {
            int numFiles = 0;
            boolean allLeaves = true;
            // post order recursive call for all of its children nodes
            Set<Map.Entry<String, FileGraph.Node>> entries = new HashSet<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                if(child.path.equals(errorFolderPath)) continue;
                reduceStructure(threshold, child);
                // update the number of files in the child node
                numFiles += child.fileCount;
                allLeaves &= child.leaf;
                // if the child node is a leaf and doesn't contain any files, it can be safely removed
                if(child.leaf && child.fileCount == 0) {
                    String key = e.getKey();
                    node.children.remove(key);
                    File emptyChild = new File(child.path);
                    if(!emptyChild.delete()) throw new IllegalStateException("couldnt delete empty child: " + child);
                }
            }

            // if all children nodes are leaves and the sum of all files is less
            // than the threshold, all children folders can be deleted and all files
            // moved into this node. If at least one child node is not a leaf, that means
            // the child folder contains more files than the threshold, because it has
            // been reorganized already (post-order traversal).
            if(allLeaves && numFiles <= threshold) {
                ICopy moveOp = new MoveReplace();
                Path currDir = Path.of(node.path);
                // iterate through all children and all files to move them
                for(FileGraph.Node child : node.children.values()) {
                    if(child.path.equals(errorFolderPath)) continue;
                    File childFolder = new File(child.path);
                    for(File f : childFolder.listFiles()) {
                        LocalDateTime ldt = DateExtractor.getDate(f);
                        String fileName = FileTools.chooseFileName(node.path, f.getName(), ldt);
                        Path from = f.toPath(), to = currDir.resolve(fileName);
                        boolean duplicate = to.toFile().exists();
                        try {
                            moveOp.execute(from, to);
                            // mark the file for increased performance in subsequent datetime accesses
                            DateExtractor.markFile(to.toFile(), ldt);
                            if(duplicate) {
                                numFiles--;
                                node.sizeTotal -= f.length();
                            }
                        } catch(IOException ioe) {
                            System.err.println("error moving during restructuring: " + f.getAbsolutePath());
                            ioe.printStackTrace();
                        }
                    }
                    if(!childFolder.delete()) throw new IllegalStateException("cant delete folder after moving all files: " + child.path);

                }

                // delete all children after moving
                node.children.clear();
                node.fileCount = numFiles;
                node.fileCountSubTree = numFiles;
            }

            // if the node has no children left, it becomes a leaf
            if(node.children.isEmpty()) node.leaf = true;
        }
    }

    /**
     * Add a new folder as a child folder to a node and also update the file graph
     * in the process.
     * @param node node/folder to create the new folder in
     * @param folderName the folder's name
     * @return the node created for the new folder
     */
    public FileGraph.Node addFolder(FileGraph.Node node, String folderName) {
        if(node == null) return null;
        String path = node.path + File.separator + folderName;
        if(!new File(path).mkdir()) return null;
        // after adding the folder, the file graph needs to be updated
        FileGraph.Node newNode = new FileGraph.Node(path, node.depth+1);
        node.children.put(path, newNode);
        node.leaf = false;
        return newNode;
    }

    /**
     * Return a list in order of all nodes lying in the path from the root of the file
     * graph and the given node.
     * @param node destination node, last node of the path
     * @return returns the path from root to given node in a list
     */
    public List<FileGraph.Node> getPathToNode(FileGraph.Node node) {
        // get the folder's names from the absolute path
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

    /**
     * Get the node from a java API file object. This function uses the folder's name
     * and searches the file graph for a node with the same absolute path as folder.
     * @param folder
     * @return
     */
    public FileGraph.Node getNode(File folder) {
        String fullPath = folder.getAbsolutePath();
        FileGraph.Node root = fileGraph.getRoot();
        // if the folder is not in the file graph subtree, stop
        if(!fullPath.startsWith(root.path)) return null;

        FileGraph.Node node = root;
        StringBuilder key = new StringBuilder(node.path);
        // get the folders from the absolute tree and split inbetween file separators, linux: /, windows: \\
        String[] folders = fullPath.substring(fileGraph.getRoot().path.length()+1).split(Pattern.quote(File.separator));
        // start from the root and search for a path to the node
        for(int i = 0; i < folders.length; i++) {
            key.append(File.separator).append(folders[i]);
            node = node.children.get(key.toString());
            if(node == null) return null;
        }

        return node;
    }
}
