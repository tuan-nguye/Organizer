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
    private Map<FileErrors, List<String>> invalidFiles = new HashMap<>();
    private Map<FolderErrors, List<String>> invalidFolders = new HashMap<>();

    public ModelChecker(Configuration config) {
        this.graph = new FileGraph(Configuration.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    // function that dfs through all files and folders and saves errors
    // in a structure, which can be returned
    public void checkAll(boolean chk_files, boolean chk_folders) {
        if(!chk_files && !chk_folders) return;
        invalidFiles.clear();
        for(FileErrors fe : FileErrors.values()) invalidFiles.put(fe, new ArrayList<>());
        invalidFolders.clear();
        for(FolderErrors fe : FolderErrors.values()) invalidFolders.put(fe, new ArrayList<>());
        checkAllDfs(graph.getRoot(), new ArrayList<>(), chk_files, chk_folders);
    }

    // recursive implementation of checkAll function
    // works on filegraph nodes to avoid IO operations
    private void checkAllDfs(FileGraph.Node node, List<String> path, boolean chk_files, boolean chk_folders) {
        // add folder name to path
        String folderName = node == graph.getRoot() ? "" : node.path.substring(node.path.lastIndexOf(File.separator)+1);
        path.add(folderName);

        if(node.leaf) {
            if(chk_folders) validFolderStructure(path);
            File leaf_folder = new File(node.path);
            if(!leaf_folder.exists()) throw new IllegalStateException("graph structure error: leaf folder doesn't exist wtf??");
            for(File f : leaf_folder.listFiles())
            {
                if(chk_folders && f.isDirectory()) invalidFolders.get(FolderErrors.FOLDER_IN_LEAF).add(f.getAbsolutePath());
                if(chk_files) checkFile(node, f);
            }
        } else {
            if(chk_folders) checkFolder(node);
            for(FileGraph.Node child : node.children.values()) checkAllDfs(child, path, chk_files, chk_folders);
        }

        // pop folder name out
        path.remove(path.size()-1);
    }

    public Map<FileErrors, List<String>> getFileErrors() {
        return new HashMap<>(this.invalidFiles);
    }

    public Map<FolderErrors, List<String>> getFolderErrors() {
        return new HashMap<>(this.invalidFolders);
    }

    // check file for correct folder and in leaf folder
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        if(!correctFolder(parentNode.path, file)) {
            invalidFiles.get(FileErrors.WRONG_FOLDER).add(file.getAbsolutePath());
            return false;
        } else if(!parentNode.leaf) {
            invalidFiles.get(FileErrors.NOT_IN_LEAF).add(file.getAbsolutePath());
        }

        return true;
    }

    public boolean correctFolder(String path, File file) {
        if(path.equals(graph.getRoot().path)) return true;

        DateIterator it = new DateIterator(FileTools.dateTime(file.lastModified()));
        String folderName = path.substring(path.lastIndexOf(File.separator)+1);
        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    // check folder for valid name, num of files <= threshold, no empty folders (maybe?)
    public boolean checkFolder(FileGraph.Node folderNode) {
        if(!validFolderName(FileTools.getFolderNameWithoutPrefix(graph.getRoot().path, folderNode.path))) {
            invalidFolders.get(FolderErrors.INVALID_NAME).add(folderNode.path);
        } else if(!validNumOfFiles(folderNode)) {
            invalidFolders.get(FolderErrors.ABOVE_THRESHOLD).add(folderNode.path);
        }

        return true;
    }

    public boolean validFolderName(String folderName) {
        if(folderName.isEmpty()) return true;

        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String folderSplitTime = folderSplit[i];
            if(!validRange(i+1, folderSplitTime));
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
            int num = Integer.parseInt(time);
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
