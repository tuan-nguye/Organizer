package com.org.util.consistency;

import com.org.observer.Observer;
import com.org.observer.Subject;
import com.org.organizer.copy.ICopy;
import com.org.organizer.copy.Move;
import com.org.organizer.copy.MoveReplace;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;
import com.org.util.graph.FileGraphOperation;
import com.org.util.time.DateExtractor;
import com.org.util.time.DateIterator;
import com.org.util.time.DateTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class that implements all repair functionality for a file graph. It takes the output
 * of the ModelChecker and fixes all inconsistencies in the model. A progress bar can
 * be attached to follow the progress.
 */
public class ModelFixer implements Subject<Integer> {
    // folder size threshold
    private int threshold;

    // subject/observer stuff, count number of errors fixed
    private List<Observer> obs = new ArrayList<>();
    private int errorsFixed = 0;
    // counts how many errors are associated with this node/folder
    private Map<FileGraph.Node, Integer> folderErrorCountMap = new HashMap<>();
    // complex file graph operation functionality
    FileGraphOperation fileGraphOperation;
    FileGraph fileGraph;
    // repository root path as string
    String rootPath;

    /**
     * ModelFixer constructor
     * @param config configuration object
     */
    public ModelFixer(Configuration config) {
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
        rootPath = config.PROPERTY_FILE_PATH_STRING;
        fileGraph = FileGraphFactory.get(rootPath);
        fileGraphOperation = new FileGraphOperation(fileGraph);
    }

    /**
     * This function goes through all errors and folders in the map and fixes
     * them while updating the number of successfully fixed folders.
     * @param errors
     */
    public void fixStructure(Map<ModelError, List<FileGraph.Node>> errors) {
        // reset structures
        errorsFixed = 0;
        folderErrorCountMap.clear();
        // look for the error folder
        fixErrorFolder(errors);
        // build a map to see how many errors are gone after fixing a folder
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            for(FileGraph.Node n : e.getValue()) {
                folderErrorCountMap.put(n, folderErrorCountMap.getOrDefault(n, 0)+1);
            }
        }
        // attempt to change folder names first, if folders have been
        // renamed by accident. this avoids move/copy operation
        System.out.println("attempting to restore folders...");
        Set<FileGraph.Node> unrestorableFolders = fixFolders(errors);
        // folder structures that can't be fixed
        // copy all files to their correct location
        System.out.println("moving files...");
        fixFiles(errors, unrestorableFolders);
        System.out.println("reducing structure...");
        // remove empty leaf folders and reduce folders
        reduceStructure();
    }

    /**
     * Check that the error folder exists. If it doesn't, create one in the root
     * directory.
     * @param errors
     */
    private void fixErrorFolder(Map<ModelError, List<FileGraph.Node>> errors) {
        if(errors.get(ModelError.ERROR_FOLDER_MISSING).size() == 0) return;
        fileGraphOperation.addFolder(fileGraph.getRoot(), "error");
    }

    /**
     * Try renaming folders to remove errors. This can save unnecessary copy/move
     * operation and improves the performance. Folders that are impossible to fix
     * that way are stored in a set and returned for later repair phases.
     * @param errors
     * @return set of folders/nodes that can't be fixed by renaming
     */
    public Set<FileGraph.Node> fixFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> faultyFolders = getFaultyFolders(errors);
        Set<FileGraph.Node> toRestore = getRestorableFolders(errors);

        // iterate through all folders that could be restored and attempt to repair
        for(FileGraph.Node fn : toRestore) {
            // maximum depth of a node is the 'seconds' time unit, anything past
            // that is not fixable
            if(fn.depth >= 6) continue;
            String original = fn.path;
            List<FileGraph.Node> path = fileGraphOperation.getPathToNode(fn);
            boolean restored = restoreFolder(fn, path, faultyFolders);

            // if restoration was possible, update the file graph to the new state
            if(restored) updateFolders();
            if(!fn.path.equals(original)) {
                updateFolders();
                // check how many errors could be fixed by that method
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

    /**
     * Get a set of all nodes/folders that are faulty and are assumed to be
     * invalid. All nodes stored in here can't be considered for validating
     * sibling, parent, children folders.
     * @param errors map with all existing errors
     * @return HashSet containing nodes, which include the folders
     */
    private Set<FileGraph.Node> getFaultyFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> faultyFolders = new HashSet<>();
        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_STRUCTURE)) {
            List<FileGraph.Node> path = fileGraphOperation.getPathToNode(fn);
            faultyFolders.addAll(path);
        }

        faultyFolders.addAll(errors.get(ModelError.INVALID_FOLDER_NAME));
        faultyFolders.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));

        return faultyFolders;
    }

    /**
     * Get a set of all nodes that could potentially be restored. This includes all folders
     * in an incorrect path, folders containing dates and times that don't match, or folders
     * having an invalid name.
     * @param errors
     * @return
     */
    private Set<FileGraph.Node> getRestorableFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        Set<FileGraph.Node> toRestore = new HashSet<>();
        toRestore.addAll(errors.get(ModelError.INVALID_FOLDER_STRUCTURE));
        toRestore.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));
        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_NAME)) if(fn.leaf) toRestore.add(fn);

        return toRestore;
    }

    /**
     * Restore all folders in the path by first attempting to fix the leaf folder.
     * If the leaf folder is correct, this can be used an anchor to fix all parent
     * folders. If the leaf folder in unfixable, then all other attempts are useless.
     * @param node start node (leaf)
     * @param path all nodes lying in its path in the graph
     * @param faultyFolders set of all folders that are invalid
     * @return
     */
    private boolean restoreFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        if(!restoreLeafFolder(node, path, faultyFolders)) return false;
        if(!restoreInnerFolders(node, path, faultyFolders)) return false;
        return true;
    }

    /**
     * Attempt to fix the leaf folder if all files in it share the same datetime.
     * Use that to get the correct folder name and validate the name with the
     * valid sibling folders. If one of the siblings already has that name the
     * folder is not repairable.
     * @param node leaf node
     * @param path path to the leaf node
     * @param faultyFolders set of all invalid folders
     * @return true if the leaf folder could be restored, false otherwise
     */
    private boolean restoreLeafFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        // if all files in the folder have the same date, fixing is possible
        if(!allAreSameDate(node)) return false;

        // generate the correct folder name from one of its files
        File leafFolder = new File(node.path);
        File[] files = leafFolder.listFiles(a -> a.isFile());
        if(files.length == 0) return false;
        String folderName = DateTools.folderName(files[0], node.depth);

        // validate with sibling nodes
        if(path.size() >= 3 && !validateWithSiblings(path.get(path.size()-2), node, folderName, faultyFolders)) {
            return false;
        }

        // attempt to rename if the new name is different from the original
        if(!leafFolder.getName().equals(folderName) && !renameFolder(node, folderName)) return false;

        return true;
    }

    /**
     * Attemp to restore inner folders by renaming them. The algorithm goes from
     * the leaf node towards the root. It is assumed that the subsequent
     * folders are already correct, so the folder name can be extracted from the
     * child's name. Renaming is only executed if the new folder name is consistent
     * with all of its sibling folders, e.g. a folder named 2020_feb cannot be valid
     * if a valid sibling's name is 2021_feb. That means the whole subdirectory tree
     * should be for the year 2020.
     * @param node
     * @param path
     * @param faultyFolders
     * @return
     */
    private boolean restoreInnerFolders(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        // save a reference to the following node in the path
        FileGraph.Node nextNode = node;

        // iterate in reverse order from leaf to the root, but not including the root
        for(int i = path.size()-2; i >= 1; i--) {
            // get the folder's new name from its child
            FileGraph.Node currNode = path.get(i);
            File currFolder = new File(currNode.path);
            String newFolderName = correctPreviousFolderName(nextNode.path);
            // validate the new name with its correctly named siblings
            if(!validateWithSiblings(path.get(i-1), currNode, newFolderName, faultyFolders)) {
                return false;
            }
            // if the new folder name is different from its current name, attempt to rename
            if(!newFolderName.equals(currFolder.getName()) && !renameFolder(currNode, newFolderName)) return false;

            nextNode = currNode;
        }

        return true;
    }

    /**
     * Check all file's datetime stamps and find out whether all have the same
     * date up until the node's depth.
     * @param node node/folder in which the files are stored
     * @return true if there are files in the folder, and they are
     * all from the same date until the node's depth
     */
    private boolean allAreSameDate(FileGraph.Node node) {
        // get all the folder's files as objects
        File folder = new File(node.path);
        StringBuilder folderNameBuilder = new StringBuilder();
        File[] files = folder.listFiles(a -> a.isFile());

        // make sure that the folder exists
        if(files == null) return false;
        // if there are no files,  it's automatic true
        else if(files.length == 0) return true;

        // get the date from the first file in the array, doesn't matter which one
        // and build the folder's correct name
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

        // check that all files are in the correct folder
        for(File f : files) {
            if(!correctFolder(folderName, f)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if a folder name is valid considering the valid sibling folder's names.
     * @param parent parent node
     * @param node node whose name is to be changed
     * @param folderName new folder name
     * @param faultyNodes set with all invalid folders. if a sibling is invalid they
     *                    can't be used for validation
     * @return true if the new folder name is conform with the other
     * valid sibling nodes
     */
    private boolean  validateWithSiblings(FileGraph.Node parent, FileGraph.Node node, String folderName, Set<FileGraph.Node> faultyNodes) {
        if(node.depth <= 1) return true;

        String prefix = folderName.substring(0, folderName.lastIndexOf('_'));
        for(FileGraph.Node sibling : parent.children.values()) {
            if(faultyNodes.contains(sibling)) continue;
            String siblingFolder = FileTools.getNameWithoutPrefix(rootPath, sibling.path);
            if(!siblingFolder.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the correct folder name considering the next folder's path. the correct folder given
     * the next folder name, e.g. if folderName = "2008_jan" then the correct previous folder is "2008"
     * @param folderPath the absolute path of one of its subfolders
     * @return the correct folder name
     */
    private String correctPreviousFolderName(String folderPath) {
        String folderName = FileTools.getNameWithoutPrefix(rootPath, folderPath);
        // the time units of the folders are delimited by underscores
        // so cut off the last unit
        int idxUnderscore = folderName.lastIndexOf("_");
        if(idxUnderscore != -1) folderName = folderName.substring(0, idxUnderscore);
        else folderName = "";

        return folderName;
    }

    /**
     * Rename the folder associated with a node and update its path.
     * @param node node to be renamed
     * @param newFolderName new folder name
     * @return true if it was successful, false otherwise
     */
    private boolean renameFolder(FileGraph.Node node, String newFolderName) {
        File folder = new File(node.path);
        File renamedFolder = new File(folder.getParent(), newFolderName);
        if(!folder.renameTo(renamedFolder)) {
            return false;
        }

        node.path = renamedFolder.getAbsolutePath();
        return true;
    }

    /**
     * Update the entire filegraph structure. Usually called after renaming
     * folder names.
     */
    private void updateFolders() {
        FileGraph.Node root = fileGraph.getRoot();
        updateFolders(root, new StringBuilder(root.path));
    }

    /**
     * Recursive implementation of the updateFolders() function. It's implemented as
     * a post order traversal algorithm. It first updates all children nodes and then
     * updates the keys for its hash maps accordingly.
     * @param node current node
     * @param path string builder for keeping track of the current absolute path
     */
    private void updateFolders(FileGraph.Node node, StringBuilder path) {
        String folderName = FileTools.getNameWithoutPrefix(rootPath, node.path);
        int originalLength = path.length();
        if(!folderName.isEmpty()) path.append(File.separator).append(folderName);
        node.path = path.toString();

        // leaf nodes don't need to be updated
        if(!node.leaf) {
            // go through each child entry
            List<Map.Entry<String, FileGraph.Node>> entries = new ArrayList<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                updateFolders(child, path);
                String key = e.getKey();
                // after the update, remove the old key and add the updated key to the map
                node.children.remove(key);
                node.children.put(child.path, child);
            }
        }

        // restore old state after being done with a folder
        path.setLength(originalLength);
    }

    /**
     * Checks if a file is in the correct folder, e.g. matches the date parsed
     * in the folder's name.
     * @param folderName the folder's name
     * @param f file to be checked
     * @return true if the datetime is correct, false otherwise
     */
    private boolean correctFolder(String folderName, File f) {
        LocalDateTime ldt = DateExtractor.getDate(f);
        if(ldt == null) return false;
        DateIterator it = new DateIterator(ldt);
        // the time units in the folder's name are delimited with underscores
        String[] folderSplit = folderName.split("_");

        // check that each time unit is correct
        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    /**
     * fix files in the unrestorable folders by moving them to their correct location. Moving them
     * to their correct folder can lead to exceeding the number of files threshold. After Moving them,
     * the folders are checked and potentially reorganized if number of files is too high.
     * @param errors map containing all errors and all folders associated to them
     * @param unrestorableFolders set of nodes/folders that are impossible to restore by renaming
     */
    public void fixFiles(Map<ModelError, List<FileGraph.Node>> errors, Set<FileGraph.Node> unrestorableFolders) {
        // union of invalid folder name, above threshold, and files in non leaf folder
        unrestorableFolders.addAll(errors.get(ModelError.FILES_IN_NON_LEAF));
        // set to store folders exceeding their threshold
        Set<FileGraph.Node> foldersAboveThreshold = new HashSet<>();
        for(FileGraph.Node invalidFolder : unrestorableFolders) {
            moveFilesToCorrectLocation(invalidFolder, foldersAboveThreshold);
            // update the folder's file count
            if(invalidFolder.leaf) invalidFolder.fileCount = FileTools.countDirectFiles(new File(invalidFolder.path));
            else invalidFolder.fileCount = 0;
            // update the errors that have been fixed and notify all observers
            errorsFixed += folderErrorCountMap.getOrDefault(invalidFolder, 0);
            folderErrorCountMap.remove(invalidFolder);
            notifyObservers();
        }

        // after all files are in their correct folder, there might be too many in some of them
        System.out.println("reorganizing...");
        foldersAboveThreshold.addAll(errors.get(ModelError.FOLDER_ABOVE_THRESHOLD));
        for(FileGraph.Node folder : foldersAboveThreshold) {
            if(folder.fileCount > threshold) fileGraphOperation.reorganize(folder, threshold);
            // update the errors that have been fixed and notify all observers
            errorsFixed += folderErrorCountMap.getOrDefault(folder, 0);
            folderErrorCountMap.remove(folder);
            notifyObservers();
        }
    }

    /**
     * move all files contained in the folder to their correct location. This function does not
     * reorganize them because the threshold will be disregarded.
     * @param folderNode folder node containing the path to the folder
     * @param foldersAboveThreshold set where all folders which exceed the threshold are stored for later processing
     */
    private void moveFilesToCorrectLocation(FileGraph.Node folderNode, Set<FileGraph.Node> foldersAboveThreshold) {
        File folder = new File(folderNode.path);
        ICopy move = new MoveReplace();

        for(File file : folder.listFiles(f -> f.isFile())) {
            FileGraph.Node correctNode = fileGraphOperation.copyFile(move, file);
            if(correctNode == null) {
                if(!file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING))
                    System.err.println("modelfixer: error when moving file to correct location or duplicate");
            } else {
                if(correctNode.fileCount > threshold) foldersAboveThreshold.add(correctNode);
            }

        }
    }

    /**
     * Iterate through the entire file graph to find the folders not having enough
     * files to be split up into subfolders, so they can be summed up. And also
     * search for all folders that are empty to remove them. This function minimizes
     * the folder structure according to the maximum number of files indicated by
     * the threshold.
     */
    public void reduceStructure() {
        fileGraphOperation.reduceStructure(threshold);
    }

    /**
     * Register an observer
     * @param o
     */
    @Override
    public void register(Observer o) {
        obs.add(o);
    }

    /**
     * Unregister an observer
     * @param o
     */
    @Override
    public void unregister(Observer o) {
        obs.remove(o);
    }

    /**
     * Notify all observers if the internal state changed.
     */
    @Override
    public void notifyObservers() {
        for(Observer ob : obs) ob.update();
    }

    /**
     * Get the current internal state of the subject by returning the number of
     * errors that already have been fixed.
     * @return
     */
    @Override
    public Integer getState() {
        return errorsFixed;
    }
}
