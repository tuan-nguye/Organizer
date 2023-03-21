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
        public int depth;
        public boolean leaf = true;
        public Map<String, FileNode> children = new HashMap<>();

        public FileNode(String path, int depth) {
            this.path = path;
            this.depth = depth;
        }

        @Override
        public String toString() {
            return String.format("%s, files: %d, leaf: %b", path, fileCount, leaf);
        }
    }

    FileNode root;

    public FileGraph(String rootStr) {
        if(!validRoot(rootStr)) throw new IllegalArgumentException(rootStr + " is not a valid destination root");
        root = new FileNode(rootStr, 0);
        update(root);
    }

    public boolean validRoot(String rootStr) {
        File root = new File(rootStr);
        if(!root.isDirectory()) return false;
        File configFile = new File(rootStr, Configuration.PROPERTY_FILE_NAME_STRING);
        if(!configFile.exists()) return false;
        return true;
    }

    public void update(FileNode node) {
        File file = new File(node.path);
        if(!file.isDirectory()) return;
        int fileCount = 0;

        for(File child : file.listFiles()) {
            if(child.isFile()) {
                if(child.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
                fileCount++;
            } else {
                String nextStr = child.getAbsolutePath();
                if(!node.children.containsKey(nextStr)) node.children.put(nextStr, new FileNode(nextStr, node.depth+1));
                FileNode nextNode = node.children.get(nextStr);
                update(nextNode);
            }
        }

        node.fileCount = fileCount;
        node.leaf = fileCount != 0;
    }

    public void printFileStructure() {
        printFileStructure(root, 0);
    }

    private void printFileStructure(FileNode node, int depth) {
        System.out.println("\t".repeat(depth) + node);
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
        StringBuilder folderName = new StringBuilder();
        boolean first = true;

        while(it.hasNext()) {
            if(first) first = false;
            else folderName.append("_");
            folderName.append(it.next());
            path.append(File.separator).append(folderName);
            if(!node.children.containsKey(path.toString())) {
                if(!node.leaf) {
                    FileNode next = new FileNode(path.toString(), node.depth+1);
                    node.children.put(next.path, next);
                    node = next;
                }
                break;
            }
            node = node.children.get(path.toString());
        }

        return node;
    }
}
