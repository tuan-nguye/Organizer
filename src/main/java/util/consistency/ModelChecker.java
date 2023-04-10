package util.consistency;

import parser.Configuration;
import util.graph.FileGraph;

import java.io.File;

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
        if(!inCorrectFolder(parentNode.path, file)) {
            System.out.printf("%s in incorrect folder\n", file.getAbsolutePath());
            return false;
        } else if(!parentNode.leaf) {
            System.out.printf("%s not in leaf folder\n", file.getAbsolutePath());
        }

        return true;
    }

    public boolean inCorrectFolder(String path, File file) {
        // TODO
        return false;
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

    public boolean validFolderName(String folderPath) {
        // TODO
        return true;
    }

    public boolean validNumOfFiles(FileGraph.Node folder) {
        return folder.fileCount <= threshold;
    }
}
