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
    private Map<ModelError, List<String>> errors = new HashMap<>();

    public ModelChecker(Configuration config) {
        this.graph = new FileGraph(Configuration.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    // function that dfs through all files and folders and saves errors
    // in a structure, which can be returned
    public void checkAll(boolean chk_files, boolean chk_folders) {
        if(!chk_files && !chk_folders) return;
        errors.clear();
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        graph.update(graph.getRoot());
        checkAllDfs(graph.getRoot(), new ArrayList<>(), chk_files, chk_folders);
    }

    // recursive implementation of checkAll function
    // works on filegraph nodes to avoid IO operations
    private void checkAllDfs(FileGraph.Node node, List<String> path, boolean chk_files, boolean chk_folders) {
        // add folder name to path
        String folderName = FileTools.getFolderNameWithoutPrefix(graph.getRoot().path, node.path);
        path.add(folderName);
        if(chk_folders) checkFolder(node);

        if(node.leaf) {
            if(chk_folders && !validFolderStructure(path)) errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(node.path);
            File leaf_folder = new File(node.path);
            if(!leaf_folder.exists()) throw new IllegalStateException("graph structure error: leaf folder doesn't exist wtf??");
            for(File f : leaf_folder.listFiles())
            {
                if(chk_files) checkFile(node, f);
            }
        } else {
            for(FileGraph.Node child : node.children.values()) checkAllDfs(child, path, chk_files, chk_folders);
        }

        // pop folder name out
        path.remove(path.size()-1);
    }

    public Map<ModelError, List<String>> getErrors() {
        return new HashMap<>(this.errors);
    }

    // check file for correct folder and in leaf folder
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        if(!correctFolder(parentNode.path, file) || !parentNode.leaf) {
            errors.get(ModelError.FILE_IN_WRONG_FOLDER).add(file.getAbsolutePath());
            return false;
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
            errors.get(ModelError.INVALID_FOLDER_NAME).add(folderNode.path);
        }
        if(!validNumOfFiles(folderNode)) {
            errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).add(folderNode.path);
        }
        if(!folderNode.leaf && folderNode.fileCount != 0) {
            errors.get(ModelError.FILES_IN_NON_LEAF).add(folderNode.path);
        }

        return true;
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
