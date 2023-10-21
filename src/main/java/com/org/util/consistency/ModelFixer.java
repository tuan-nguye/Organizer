package com.org.util.consistency;

import com.org.observer.Observer;
import com.org.observer.Subject;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.ICopy;
import com.org.organizer.copy.Move;
import com.org.organizer.copy.MoveReplace;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;
import com.org.util.time.DateExtractor;
import com.org.util.time.DateIterator;
import com.org.util.time.DateTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public class ModelFixer implements Subject<Integer> {
    private FileGraph fileGraph;
    private int threshold;
    private FileGraph.Node errorNode;

    // subject/observer stuff, count number of errors fixed
    private List<Observer> obs = new ArrayList<>();
    private int errorsFixed = 0;
    private Map<FileGraph.Node, Integer> folderErrorCountMap = new HashMap<>();

    public ModelFixer(Configuration config) {
        fileGraph = FileGraphFactory.get(config.PROPERTY_FILE_PATH_STRING);
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        FileGraph.Node root = fileGraph.getRoot();
        errorNode = root.children.get(root.path + File.separator + Configuration.ERROR_FOLDER_NAME);
    }

    public void fixStructure(Map<ModelError, List<FileGraph.Node>> errors) {
        errorsFixed = 0;
        folderErrorCountMap.clear();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            for(FileGraph.Node n : e.getValue()) {
                folderErrorCountMap.put(n, folderErrorCountMap.getOrDefault(n, 0)+1);
            }
        }
        System.out.println("attempting to restore folders...");
        Set<FileGraph.Node> unrestorableFolders = fixFolders(errors);
        System.out.println("moving files...");
        fixFiles(errors, unrestorableFolders);
        System.out.println("reducing structure...");
        reduceStructure();
    }

    private Set<FileGraph.Node> fixFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> faultyFolders = getFaultyFolders(errors);
        Set<FileGraph.Node> toRestore = getRestorableFolders(errors);

        for(FileGraph.Node fn : toRestore) {
            if(fn.depth >= 6) continue;
            String original = fn.path;
            List<FileGraph.Node> path = getPathToNode(fn);
            boolean restored = restoreFolder(fn, path, faultyFolders);

            if(restored) updateFolders();
            if(!fn.path.equals(original)) {
                updateFolders();
                path = getPathToNode(fn);
                for(FileGraph.Node n : path) {
                    if(faultyFolders.remove(n)) {
                        errorsFixed += folderErrorCountMap.getOrDefault(n, 0);
                        folderErrorCountMap.remove(n);
                    }
                }
                System.out.printf("successfully restored structure:\n%s -> %s\n", original, fn.path);
                notifyObservers();
            }
        }

        return faultyFolders;
    }

    private Set<FileGraph.Node> getFaultyFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> faultyFolders = new HashSet<>();
        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_STRUCTURE)) {
            List<FileGraph.Node> path = getPathToNode(fn);
            faultyFolders.addAll(path);
        }

        faultyFolders.addAll(errors.get(ModelError.INVALID_FOLDER_NAME));
        faultyFolders.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));

        return faultyFolders;
    }

    private Set<FileGraph.Node> getRestorableFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> toRestore = new HashSet<>();
        toRestore.addAll(errors.get(ModelError.INVALID_FOLDER_STRUCTURE));
        toRestore.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));
        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_NAME)) if(fn.leaf) toRestore.add(fn);

        return toRestore;
    }

    private boolean restoreFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        if(!restoreLeafFolder(node, path, faultyFolders)) return false;
        if(!restoreInnerFolders(node, path, faultyFolders)) return false;
        return true;
    }

    private boolean restoreLeafFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        if(!allAreSameDate(node)) return false;

        File leafFolder = new File(node.path);
        File[] files = leafFolder.listFiles(a -> a.isFile());
        if(files.length == 0) throw new IllegalStateException("folder size is 0");
        String folderName = DateTools.folderName(files[0], node.depth);


        // check sibling nodes
        if(path.size() >= 3 && !validateWithSiblings(path.get(path.size()-2), node, folderName, faultyFolders)) {
            return false;
        }

        // attempt rename
        if(!leafFolder.getName().equals(folderName) && !renameFolder(node, folderName)) return false;

        return true;
    }

    private boolean restoreInnerFolders(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        FileGraph.Node nextNode = node;

        for(int i = path.size()-2; i >= 1; i--) {
            FileGraph.Node currNode = path.get(i);
            File currFolder = new File(currNode.path);
            String newFolderName = correctPreviousFolderName(nextNode.path);


            if(!validateWithSiblings(path.get(i-1), currNode, newFolderName, faultyFolders)) {
                return false;
            }

            if(!newFolderName.equals(currFolder.getName()) && !renameFolder(currNode, newFolderName)) return false;


            nextNode = currNode;
        }

        return true;
    }

    /**
     *
     * @param node
     * @return returns the path from root to given node in a list
     */
    private List<FileGraph.Node> getPathToNode(FileGraph.Node node) {
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
     *
     * @param node
     * @return true if there are files in the folder, and they are
     * all from the same date until the node's depth
     */
    private boolean allAreSameDate(FileGraph.Node node) {
        File folder = new File(node.path);
        StringBuilder folderNameBuilder = new StringBuilder();
        File[] files = folder.listFiles(a -> a.isFile());

        if(files == null || files.length == 0) return false;

        LocalDateTime ldt = DateExtractor.getDate(files[0]);
        if(ldt == null) return false;
        DateIterator di = new DateIterator(ldt);
        boolean first = true;
        for(int i = 0; i < Math.min(node.depth, 6); i++) {
            if(first) first = false;
            else folderNameBuilder.append("_");
            folderNameBuilder.append(di.next());
        }
        String folderName = folderNameBuilder.toString();

        for(File f : files) {
            if(!correctFolder(folderName, f)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param parent
     * @param node
     * @param faultyNodes
     * @return true if the new folder name is conform with the other
     * valid sibling nodes
     */
    private boolean  validateWithSiblings(FileGraph.Node parent, FileGraph.Node node, String folderName, Set<FileGraph.Node> faultyNodes) {
        if(node.depth <= 1) return true;

        String prefix = folderName.substring(0, folderName.lastIndexOf('_'));
        for(FileGraph.Node sibling : parent.children.values()) {
            if(faultyNodes.contains(sibling)) continue;
            String siblingFolder = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, sibling.path);
            if(!siblingFolder.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param folderPath
     * @return the correct folder given the next folder name, e.g.
     * if folderName = "2008_jan" then the correct previous folder
     * is "2008"
     */
    private String correctPreviousFolderName(String folderPath) {
        String folderName = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, folderPath);
        int idxUnderscore = folderName.lastIndexOf("_");
        if(idxUnderscore != -1) folderName = folderName.substring(0, idxUnderscore);
        else folderName = "";

        return folderName;
    }

    private boolean renameFolder(FileGraph.Node node, String newFolderName) {
        File folder = new File(node.path);
        File renamedFolder = new File(folder.getParent(), newFolderName);
        if(!folder.renameTo(renamedFolder)) {
            return false;
        }

        node.path = renamedFolder.getAbsolutePath();
        return true;
    }

    private void updateFolders() {
        FileGraph.Node root = fileGraph.getRoot();
        updateFolders(root, new StringBuilder(root.path));
    }

    private void updateFolders(FileGraph.Node node, StringBuilder path) {
        String folderName = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, node.path);
        int originalLength = path.length();
        if(!folderName.isEmpty()) path.append(File.separator).append(folderName);
        node.path = path.toString();

        if(!node.leaf) {
            List<Map.Entry<String, FileGraph.Node>> entries = new ArrayList<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                updateFolders(child, path);
                String key = e.getKey();
                node.children.remove(key);
                node.children.put(child.path, child);
            }
        }

        // restore old state
        path.setLength(originalLength);
    }

    private boolean correctFolder(String folderName, File f) {
        LocalDateTime ldt = DateExtractor.getDate(f);
        if(ldt == null) return false;
        DateIterator it = new DateIterator(ldt);
        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    /**
     * fix files in the unrestorable folders by moving them to their correct location
     * @param errors
     * @param unrestorableFolders
     */
    private void fixFiles(Map<ModelError, List<FileGraph.Node>> errors, Set<FileGraph.Node> unrestorableFolders) {
        // union of invalid folder name, above threshold, and files in non leaf folder
        unrestorableFolders.addAll(errors.get(ModelError.FILES_IN_NON_LEAF));
        Set<FileGraph.Node> foldersAboveThreshold = new HashSet<>();
        for(FileGraph.Node invalidFolder : unrestorableFolders) {
            copyFilesToCorrectLocation(invalidFolder, foldersAboveThreshold);
            if(invalidFolder.leaf) invalidFolder.fileCount = FileTools.countDirectFiles(new File(invalidFolder.path));
            else invalidFolder.fileCount = 0;
            errorsFixed += folderErrorCountMap.getOrDefault(invalidFolder, 0);
            folderErrorCountMap.remove(invalidFolder);
            notifyObservers();
        }
        System.out.println("reorganizing...");
        ThresholdOrganizer org = new ThresholdOrganizer(new Move(), threshold, fileGraph.getRoot().path);
        foldersAboveThreshold.addAll(errors.get(ModelError.FOLDER_ABOVE_THRESHOLD));
        for(FileGraph.Node folder : foldersAboveThreshold) {
            if(folder.fileCount > threshold) org.reorganize(folder);
            errorsFixed += folderErrorCountMap.getOrDefault(folder, 0);
            folderErrorCountMap.remove(folder);
            notifyObservers();
        }
    }

    private void copyFilesToCorrectLocation(FileGraph.Node folderNode, Set<FileGraph.Node> foldersAboveThreshold) {
        File folder = new File(folderNode.path);
        ICopy move = new MoveReplace();

        for(File file : folder.listFiles(f -> f.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            LocalDateTime ldt = DateExtractor.getDate(file);
            FileGraph.Node correctNode;

            if(ldt != null) correctNode = fileGraph.getNode(ldt);
            else correctNode = errorNode;

            File correctFolder = new File(correctNode.path);

            if(!correctFolder.exists()) correctFolder.mkdir();
            Path from = file.toPath(), to = Path.of(correctNode.path, file.getName());
            if(from.equals(to)) continue;

            try {
                move.execute(file.toPath(), Path.of(correctNode.path, file.getName()));
                correctNode.fileCount++;
                if(correctNode.fileCount > threshold) foldersAboveThreshold.add(correctNode);
            } catch(IOException ioe) {
                System.out.println("modelfixer: error when moving file to correct location");
                ioe.printStackTrace();
            }
        }
    }

    /**
     * reduce the structure if it shouldnt be split
     * if the number of files in its children is not above the threshold
     * then it shouldn't be split, so copy all files in the child folders
     * into itself then delete children
     * dfs post order
     */
    public void reduceStructure() {
        reduceStructure(fileGraph.getRoot());
    }

    private void reduceStructure(FileGraph.Node node) {
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
                reduceStructure(child);
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
                ICopy moveOp = new Move();
                Path currDir = Path.of(node.path);
                for(FileGraph.Node child : node.children.values()) {
                    File childFolder = new File(child.path);
                    for(File f : childFolder.listFiles()) {
                        try {
                            moveOp.execute(f.toPath(), currDir.resolve(f.getName()));
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
        for(Observer ob : obs) ob.update();
    }

    @Override
    public Integer getState() {
        return errorsFixed;
    }
}
