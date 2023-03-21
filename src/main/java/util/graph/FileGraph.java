package util.graph;

import parser.Configuration;
import util.time.DateIterator;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class FileGraph {
    public static class FileNode {
        public String path;
        public int fileCount = 0;
        public Map<String, FileNode> children = new HashMap<>();

        public FileNode(String path) {
            this.path = path;
        }
    }

    FileNode root;

    public FileGraph(String rootStr) {
        if(!validRoot(rootStr)) throw new IllegalArgumentException("not a valid destination root");
        root = new FileNode(rootStr);
        update(root);
    }

    public boolean validRoot(String rootStr) {
        File root = new File(rootStr);
        if(!root.isDirectory()) return false;
        File configFile = new File(root, Configuration.PROPERTY_FILE_PATH_STRING);
        if(!configFile.exists()) return false;
        return true;
    }

    public void update(FileNode node) {
        File file = new File(node.path);
        if(!file.isDirectory()) return;
        int fileCount = 0;

        for(File child : file.listFiles()) {
            if(child.isFile()) fileCount++;
            else {
                String nextStr = child.getAbsolutePath();
                if(!node.children.containsKey(nextStr)) node.children.put(nextStr, new FileNode(nextStr));
                FileNode nextNode = node.children.get(nextStr);
                update(nextNode);
            }
        }

        node.fileCount = fileCount;
    }

    public void printFileStructure() {
        printFileStructure(root, 0);
    }

    private void printFileStructure(FileNode node, int depth) {
        System.out.println("\t".repeat(depth) + node.path.substring(node.path.lastIndexOf("/")) + ", files: " + node.fileCount);
        for(FileNode child : node.children.values()) {
            printFileStructure(child, depth+1);
        }
    }

    public FileNode getRoot() {
        return root;
    }

    public FileNode getNode(LocalDateTime dateTime) {
        DateIterator it = new DateIterator(dateTime);
        FileNode node = root;
        StringBuilder path = new StringBuilder(root.path);
        boolean first = true;

        while(it.hasNext()) {
            if(!node.children.containsKey(path.toString())) break;
            if(first) first = false;
            else path.append("_");
            path.append(it.next());
            node = node.children.get(path.toString());
        }

        return null;
    }
}
