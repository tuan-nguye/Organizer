package util.consistency;

import parser.Configuration;
import util.FileTools;
import util.graph.FileGraph;
import util.time.DateIterator;
import util.time.DateStats;

import java.io.File;
import java.util.List;

public class ModelChecker {
    private FileGraph graph;
    private Configuration config;
    private int threshold;

    public ModelChecker(Configuration config) {
        this.graph = new FileGraph(Configuration.PROPERTY_FILE_PATH_STRING);
        this.config = config;
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    // check file for correct folder and in leaf folder
    public boolean checkFile(FileGraph.Node parentNode, File file) {
        if(!correctFolder(parentNode.path, file)) {
            System.out.printf("%s in incorrect folder\n", file.getAbsolutePath());
            return false;
        } else if(!parentNode.leaf) {
            System.out.printf("%s not in leaf folder\n", file.getAbsolutePath());
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
        if(!validFolderName(folderNode.path)) {
            System.out.printf("%s is not a valid folder\n", folderNode.path);
        } else if(!validNumOfFiles(folderNode)) {
            System.out.printf("%s has more files than allowed: num=%d\n", folderNode.path, folderNode.fileCount);
        }

        return true;
    }

    public boolean validFolderName(String folderName) {
        if(folderName.isEmpty()) return true;

        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String folderSplitTime = folderSplit[i];

            if(!DateStats.unit[i].isEmpty()) {
                if(!folderSplitTime.endsWith(DateStats.unit[i])) return false;
                folderSplitTime = folderSplitTime.substring(0, folderSplitTime.length()-DateStats.unit[i].length());
            }

            if(i != 1) {
                int num = Integer.parseInt(folderSplitTime);
                if(num < DateStats.dateRange[i][0] || num > DateStats.dateRange[i][1])
                    return false;
            } else {
                if(!DateStats.monthInt.containsKey(folderSplitTime)) return false;
            }

        }

        return true;
    }

    public boolean validNumOfFiles(FileGraph.Node folder) {
        return folder.fileCount <= threshold;
    }

    public boolean validFolderStructure(List<String> folders) {
        return true;
    }
}
