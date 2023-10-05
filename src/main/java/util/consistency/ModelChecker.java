package util.consistency;

import parser.Configuration;
import util.FileTools;
import util.graph.FileGraph;
import util.time.DateIterator;
import util.time.DateStats;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelChecker {
    private FileGraph graph;
    private Configuration config;
    private int threshold;
    // saved errors after check
    private Map<ModelError, List<ModelElement>> errors = new HashMap<>();

    public ModelChecker(Configuration config) {
        this.graph = new FileGraph(Configuration.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    // function that dfs through all files and folders and saves errors
    // in a structure, which can be returned

    /**
     * iterate through the filegraph recursively
     * reduces redundant errors, so if a folder has an error all its
     * files also have errors by default
     * @param chkFiles true if files should be checked, costs more time
     * @param chkFolders true if folders should be checked
     */
    public void checkAll(boolean chkFiles, boolean chkFolders) {
        if(!chkFiles && !chkFolders) return;
        errors.clear();
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        graph.update(graph.getRoot());
        checkAllDfs(graph.getRoot(), new ArrayList<>(), chkFiles, chkFolders);
    }

    /**
     * the function recursively iterates through the filegraph and searches
     * for inconsistencies
     * @param node node to start dfs at
     * @param path list of folders on its current path
     * @param chkFiles boolean value, check files or not
     * @param chkFolders boolean value, check folders or not
     */
    private void checkAllDfs(FileGraph.Node node, List<String> path, boolean chkFiles, boolean chkFolders) {
        // add folder name to path
        String folderName = FileTools.getFolderNameWithoutPrefix(graph.getRoot().path, node.path);
        path.add(folderName);
        boolean validFolder = true;
        if(chkFolders) validFolder = checkFolder(node);

        if(node.leaf) {
            if(chkFolders && !validFolderStructure(path)) errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(new ModelElement(node));
            File leaf_folder = new File(node.path);
            if(!leaf_folder.exists()) throw new IllegalStateException("graph structure error: leaf folder doesn't exist, mismatch between filegraph and real structure");
            if(chkFiles && validFolder) {
                for(File f : leaf_folder.listFiles())
                    checkFile(node, f);
            }

        } else {
            for(FileGraph.Node child : node.children.values()) checkAllDfs(child, path, chkFiles, chkFolders);
        }

        // pop folder name out
        path.remove(path.size()-1);
    }

    public Map<ModelError, List<ModelElement>> getErrors() {
        return new HashMap<>(this.errors);
    }

    // check file for correct folder and in leaf folder
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        if(!correctFolder(parentNode, file) || !parentNode.leaf) {
            errors.get(ModelError.FILE_IN_WRONG_FOLDER).add(new ModelElement(parentNode, file.getName()));
            return false;
        }

        return true;
    }

    public boolean correctFolder(FileGraph.Node parentNode, File file) {
        if(parentNode == graph.getRoot()) return true;

        DateIterator it = new DateIterator(FileTools.dateTime(file.lastModified()));
        String folderName = parentNode.path.substring(parentNode.path.lastIndexOf(File.separator)+1);
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

        if(!validFolderName(FileTools.getFolderNameWithoutPrefix(graph.getRoot().path, folderNode.path))) {
            errors.get(ModelError.INVALID_FOLDER_NAME).add(new ModelElement(folderNode));
            validFolder = false;
        }
        if(!validNumOfFiles(folderNode)) {
            errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).add(new ModelElement(folderNode));
            validFolder = false;
        }
        if(!folderNode.leaf && folderNode.fileCount != 0) {
            errors.get(ModelError.FILES_IN_NON_LEAF).add(new ModelElement(folderNode));
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
            if(!time.endsWith(DateStats.unit[depth-1])) return false;
            time = time.substring(0, time.length()-DateStats.unit[depth].length());
        }
        if(depth != 2) {
            int num = 0;
            try {
                num = Integer.parseInt(time);
            } catch(NumberFormatException nfe) {
                return false;
            }
            if(num < DateStats.dateRange[depth-1][0] || num > DateStats.dateRange[depth-1][1])
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
}
