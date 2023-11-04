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

public class ModelChecker implements Subject<Integer> {
    private FileGraph graph;
    private Configuration config;
    private int threshold;
    // saved errors after check
    private Map<ModelError, List<FileGraph.Node>> errors = new HashMap<>();
    private String errorFolderPath;

    // subject/observer stuff
    private List<Observer> obs = new ArrayList<>();
    int foldersChecked = 0;

    public ModelChecker(Configuration config) {
        this.graph = FileGraphFactory.get(config.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    // function that dfs through all files and folders and saves errors
    // in a structure, which can be returned

    /**
     * iterate through the filegraph recursively
     * reduces redundant errors, so if a folder has an error all its
     * files also have errors by default
     */
    public void checkAll() {
        foldersChecked = 0;
        errors.clear();
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        FileGraph.Node root = graph.getRoot();
        errorFolderPath = root.path + File.separator + Configuration.ERROR_FOLDER_NAME;
        if(!new File(errorFolderPath).exists()) errors.get(ModelError.ERROR_FOLDER_MISSING).add(null);
        checkAllDfs(graph.getRoot(), new ArrayList<>());
    }

    /**
     * the function recursively iterates through the filegraph and searches
     * for inconsistencies
     *
     * @param node node to start dfs at
     * @param path list of folders on its current path
     * @return returns the sum of files in child nodes
     */
    private int checkAllDfs(FileGraph.Node node, List<String> path) {
        if(node.path.equals(errorFolderPath)) return 0;
        // add folder name to path
        String folderName = FileTools.getNameWithoutPrefix(graph.getRoot().path, node.path);
        path.add(folderName);
        boolean validFolder = checkFolder(node);
        int numFiles = 0;

        if(node.leaf) {
            if(!validFolderStructure(path)) errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(node);
            File leaf_folder = new File(node.path);
            if(!leaf_folder.exists()) throw new IllegalStateException("graph structure error: leaf folder doesn't exist, mismatch between filegraph and real structure");
            if(validFolder) {
                for(File f : leaf_folder.listFiles())
                    if(!checkFile(node, f)) {
                        errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES).add(node);
                        break;
                    }
            }
            numFiles = node.fileCount;
        } else {
            for(FileGraph.Node child : node.children.values()) {
                numFiles += checkAllDfs(child, path);
            }
        }

        if(node != graph.getRoot()) {
            if(node.leaf && numFiles == 0) errors.get(ModelError.CAN_BE_REDUCED).add(node);
            else if(!node.leaf && numFiles <= threshold) errors.get(ModelError.CAN_BE_REDUCED).add(node);
        }


        // update folder count
        foldersChecked++;
        notifyObservers();

        // pop folder name out
        path.remove(path.size()-1);
        return numFiles;
    }

    public Map<ModelError, List<FileGraph.Node>> getErrors() {
        return new HashMap<>(this.errors);
    }

    // check file for correct folder and in leaf folder
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        return parentNode.leaf && correctFolder(parentNode, file);
    }

    public boolean correctFolder(FileGraph.Node parentNode, File file) {
        if(parentNode == graph.getRoot()) return true;

        LocalDateTime ldt = DateExtractor.getDate(file);
        if(ldt == null) return false;
        DateIterator it = new DateIterator(ldt);
        String folderName = FileTools.getNameWithoutPrefix(graph.getRoot().path, parentNode.path);
        String[] folderSplit = folderName.split("_");
        if(parentNode.depth != folderSplit.length) return false;

        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    // check folder for valid name, num of files <= threshold, no empty folders (maybe?)
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
        if(!folderNode.leaf && folderNode.fileCount != 0) {
            errors.get(ModelError.FILES_IN_NON_LEAF).add(folderNode);
            validFolder = false;
        }

        return validFolder;
    }

    public boolean validFolderName(String folderName) {
        if(folderName.isEmpty()) return true;

        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String folderSplitTime = folderSplit[i];
            if(!validRange(i+1, folderSplitTime)) return false;
        }

        return true;
    }

    private boolean validRange(int depth, String time) {
        if(depth == 0) return time.isEmpty();

        if(!DateStats.unit[depth].isEmpty()) {
            if(!time.endsWith(DateStats.unit[depth])) return false;
            time = time.substring(0, time.length()-DateStats.unit[depth].length());
        }
        if(depth != 2) {
            int num = 0;
            try {
                num = Integer.parseInt(time);
            } catch(NumberFormatException nfe) {
                return false;
            }
            if(num < DateStats.dateRange[depth][0] || num > DateStats.dateRange[depth][1])
                return false;
        } else {
            if(!DateStats.monthInt.containsKey(time)) return false;
        }

        return true;
    }

    public boolean validNumOfFiles(FileGraph.Node folder) {
        return folder.fileCount <= threshold;
    }

    /**
     *
     * @param folders string name of folders (not absolute path) from root to the last one, root string must be ""
     * @return true if correct, false if wrong
     */
    public boolean validFolderStructure(List<String> folders) {
        String folderPrefix = "";

        for(int i = 0; i < folders.size(); i++) {
            String folder = folders.get(i);
            if(!folder.startsWith(folderPrefix)) return false;
            else {
                String time;
                if(i <= 1) time = folder;
                else {
                    int idx = folder.lastIndexOf('_');
                    if(idx == -1) return false;
                    time = folder.substring(idx+1);
                }
                if(!validRange(i, time)) return false;
            }

            folderPrefix = folder;
        }

        return true;
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
        for(Observer ob : obs) {
            ob.update();
        }
    }

    @Override
    public Integer getState() {
        return foldersChecked;
    }
}
