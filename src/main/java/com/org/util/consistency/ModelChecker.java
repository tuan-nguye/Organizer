package com.org.util.consistency;

import com.org.observer.Observer;
import com.org.observer.Subject;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;
import com.org.util.time.DateExtractor;
import com.org.util.time.DateIterator;
import com.org.util.time.DateStats;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class has validation functionality to search a file graph structure
 * for errors and collect them into a structure. All errors are in the enumeration
 * ModelError.
 */
public class ModelChecker implements Subject<Integer> {
    // file graph to check
    private FileGraph graph;
    // configuration object for extra information
    private Configuration config;
    // folder size threshold
    private int threshold;
    // saved errors after check in a map which maps each error a list of faulty folders
    // the error are stored here after execution and can be returned through a getter function
    private Map<ModelError, List<FileGraph.Node>> errors = new HashMap<>();
    // absolute path of the error folder as string
    private String errorFolderPath;

    // subject/observer stuff
    private List<Observer> obs = new ArrayList<>();
    int foldersChecked = 0;

    /**
     * Modelchecker constructor.
     * @param config configuration object
     */
    public ModelChecker(Configuration config) {
        this.graph = FileGraphFactory.get(config.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    /**
     * iterate through the filegraph recursively and search for errors in the model.
     * All errors are saved in the error map.
     */
    public void checkAll() {
        // reset some structures
        foldersChecked = 0;
        errors.clear();
        // initialize the error map with empty lists
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        // the error folder is always in the root of the graph
        FileGraph.Node root = graph.getRoot();
        errorFolderPath = root.path + File.separator + Configuration.ERROR_FOLDER_NAME;
        if(!new File(errorFolderPath).exists()) errors.get(ModelError.ERROR_FOLDER_MISSING).add(null);
        // start the dfs search
        checkAllDfs(graph.getRoot(), new ArrayList<>());
    }

    /**
     * the function recursively iterates through the filegraph and searches
     * for inconsistencies. The actual recursive search function is implemented
     * here.
     *
     * @param node node to start dfs at
     * @param path list of folders on its current path
     * @return returns the sum of files in child nodes
     */
    private int checkAllDfs(FileGraph.Node node, List<String> path) {
        // skip the error folder
        if(node.path.equals(errorFolderPath)) return 0;
        // add folder name to path
        String folderName = FileTools.getNameWithoutPrefix(graph.getRoot().path, node.path);
        path.add(folderName);
        // check the folder for errors
        boolean validFolder = checkFolder(node);
        // keep track of number of files in current and all subdirectories
        int numFiles = 0;

        // different checks depending on if the node is an inner node or a leaf
        if(node.leaf) {
            // check the folder path only on leaf nodes to save computations
            // redundant folder subpaths can be ignored that way and the result is still the same
            if(!validFolderStructure(path)) errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(node);
            // check that the folder actually exists, this shouldn't lead to an error
            // unless there is a mismatch between the filesystem and the filegraph
            File leaf_folder = new File(node.path);
            if(!leaf_folder.exists()) throw new IllegalStateException("graph structure error: leaf folder doesn't exist, mismatch between filegraph and real structure");
            // only check if the folder didn't already have errors to avoid redundancy
            if(validFolder) {
                // check that each file in the leaf folder is in the correct one according to their datetime stamp
                for(File f : leaf_folder.listFiles())
                    if(!checkFile(node, f)) {
                        // if the file doesn't belong in this folder, mark it as inconsistent
                        errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES).add(node);
                        break;
                    }
            }
            numFiles = node.fileCount;
        } else {
            // recursive call on inner node
            for(FileGraph.Node child : node.children.values()) {
                numFiles += checkAllDfs(child, path);
            }
        }

        // if the number of files in the subdirectories are less than the threshold
        // they aren't needed and the structure can be reduced here
        if(node != graph.getRoot()) {
            if(node.leaf && numFiles == 0) errors.get(ModelError.CAN_BE_REDUCED).add(node);
            else if(!node.leaf && numFiles <= threshold) errors.get(ModelError.CAN_BE_REDUCED).add(node);
        }

        // update folder count
        foldersChecked++;
        notifyObservers();

        // pop folder name out of the path
        path.remove(path.size()-1);
        return numFiles;
    }

    /**
     * Get the errors found by the last execution of the model checker.
     * @return HashMap which maps each error a list of folders that are faulty
     */
    public Map<ModelError, List<FileGraph.Node>> getErrors() {
        return new HashMap<>(this.errors);
    }

    /**
     * check that the file is in a leaf folder and that it's in the correct folder
     * according to its time stamp.
     * @return true if it's correct, false otherwise
     */
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        return parentNode.leaf && correctFolder(parentNode, file);
    }

    /**
     * Function that checks that the file is in the correct folder according to their
     * time stamp. The time is parsed as the folder's name.
     * @param parentNode
     * @param file
     * @return true if valid, false otherwise
     */
    public boolean correctFolder(FileGraph.Node parentNode, File file) {
        // root node is always correct
        if(parentNode == graph.getRoot()) return true;

        // extract the file's date and create an iterator for the time units
        LocalDateTime ldt = DateExtractor.getDate(file);
        if(ldt == null) return false;
        DateIterator it = new DateIterator(ldt);
        // the date of the folder is parsed in its name, so split it up
        String folderName = FileTools.getNameWithoutPrefix(graph.getRoot().path, parentNode.path);
        String[] folderSplit = folderName.split("_");
        // check if the folders time is in the correct depth of graph
        if(parentNode.depth != folderSplit.length) return false;

        // check that the dates of the folder and the files match
        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    /**
     * check folder for valid name, num of files <= threshold, and that
     * files should only be stored in leaf folders. The error are stored
     * in the error map
     * @param folderNode
     * @return true if the folder has no errors
     */
    public boolean checkFolder(FileGraph.Node folderNode) {
        boolean validFolder = true;

        if(!validFolderName(FileTools.getNameWithoutPrefix(graph.getRoot().path, folderNode.path))) {
            errors.get(ModelError.INVALID_FOLDER_NAME).add(folderNode);
            validFolder = false;
        }

        if(!validNumOfFiles(folderNode)) {
            errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).add(folderNode);
            validFolder = false;
        }
        // inner nodes shouldn't have files
        if(!folderNode.leaf && folderNode.fileCount != 0) {
            errors.get(ModelError.FILES_IN_NON_LEAF).add(folderNode);
            validFolder = false;
        }

        return validFolder;
    }

    /**
     * Check the folder's name for correctness. year_month_day-of-month and so on.
     * time units are in order of largest to smallest and delimited with underscores.
     * @param folderName
     * @return
     */
    public boolean validFolderName(String folderName) {
        if(folderName.isEmpty()) return true;
        // the time units are split by underscores
        String[] folderSplit = folderName.split("_");

        // go through each time unit and check that the numbers are valid
        // e.g. day of month between 0 and 31 for july
        for(int i = 0; i < folderSplit.length; i++) {
            String folderSplitTime = folderSplit[i];
            if(!validRange(i+1, folderSplitTime)) return false;
        }

        return true;
    }

    /**
     * Check if the time unit string is formatted correctly for its depth in the graph.
     * @param depth depth in the file graph
     * @param time time as string
     * @return
     */
    private boolean validRange(int depth, String time) {
        // the root doesn't have a time
        if(depth == 0) return time.isEmpty();

        // check the time string for correct unit of time, e.g. hour values and with 'h'
        // split of the unit
        if(!DateStats.unit[depth].isEmpty()) {
            if(!time.endsWith(DateStats.unit[depth])) return false;
            time = time.substring(0, time.length()-DateStats.unit[depth].length());
        }
        // depth==2 are the months and are not given as numbers
        if(depth != 2) {
            // parse the number into an int
            int num = 0;
            try {
                num = Integer.parseInt(time);
            } catch(NumberFormatException nfe) {
                return false;
            }
            // check that the number is inside the valid range for the time unit
            if(num < DateStats.dateRange[depth][0] || num > DateStats.dateRange[depth][1])
                return false;
        } else {
            // for months, check that the month name is correct
            if(!DateStats.monthInt.containsKey(time)) return false;
        }

        return true;
    }

    /**
     * Check that the number of files are correct.
     * @param folder
     * @return
     */
    public boolean validNumOfFiles(FileGraph.Node folder) {
        return folder.fileCount <= threshold;
    }

    /**
     * Check that the folder structure is correct. A valid folder structure looks like this:
     * /2020/2020_feb/2020_feb_2/2020_feb_2_16h. The prefix of subfolders is always the parent
     * folder's name. For example, the folder 2020_feb should not be a subfolder for 2022.
     * @param folders string name of folders (not absolute path) from root to the last one, root string must be ""
     * @return true if correct, false if wrong
     */
    public boolean validFolderStructure(List<String> folders) {
        // keeps track of the parent folder's name
        String folderPrefix = "";
        // iterate through the folders from lowest to highest depth in the graph
        for(int i = 0; i < folders.size(); i++) {
            String folder = folders.get(i);
            // check that the prefix matches the parent folder's name
            if(!folder.startsWith(folderPrefix)) return false;
            else {
                // split off the time after the prefix
                String time;
                if(i <= 1) time = folder;
                else {
                    int idx = folder.lastIndexOf('_');
                    if(idx == -1) return false;
                    time = folder.substring(idx+1);
                }
                // check that the time is correct according to its depth
                if(!validRange(i, time)) return false;
            }

            folderPrefix = folder;
        }

        return true;
    }

    /**
     * Register an observer.
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
     * Notify all observers that the subject's state changed.
     */
    @Override
    public void notifyObservers() {
        for(Observer ob : obs) {
            ob.update();
        }
    }

    /**
     * get the current object's state.
     * @return
     */
    @Override
    public Integer getState() {
        return foldersChecked;
    }
}
