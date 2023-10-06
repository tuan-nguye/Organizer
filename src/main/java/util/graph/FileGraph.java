package util.graph;

import parser.Configuration;
import util.time.DateIterator;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileGraph {
    public static class Node {
        public String path;
        public int fileCount = 0;
        public int depth;
        public boolean leaf = true;
        public Map<String, Node> children = new HashMap<>();

        public Node(String path, int depth) {
            this.path = path;
            this.depth = depth;
        }

        @Override
        public String toString() {
            return String.format("%s, files: %d, leaf: %b", path, fileCount, leaf);
        }
    }

    Node root;

    public FileGraph(String rootStr) {
        root = new Node(Path.of(rootStr).toAbsolutePath().toString(), 0);
        update(root);
    }

    public void update(Node node) {
        File file = new File(node.path);
        if(!file.exists() || !file.isDirectory()) {
            node.children.clear();
            return;
        }
        int fileCount = 0;
        Set<String> toRemove = new HashSet<>(node.children.keySet());

        for(File child : file.listFiles()) {
            if(child.isFile()) {
                if(child.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
                fileCount++;
            } else {
                String nextStr = child.getAbsolutePath();
                if(!node.children.containsKey(nextStr)) node.children.put(nextStr, new Node(nextStr, node.depth+1));
                Node nextNode = node.children.get(nextStr);
                toRemove.remove(nextStr);
                update(nextNode);
            }
        }

        for(String rm : toRemove) node.children.remove(rm);
        node.fileCount = fileCount;
        node.leaf = node.children.isEmpty();
    }

    public void printFileStructure() {
        printFileStructure(root, 0);
    }

    private void printFileStructure(Node node, int depth) {
        System.out.println("\t".repeat(depth) + node);
        for(Node child : node.children.values()) {
            printFileStructure(child, depth+1);
        }
    }

    public Node getRoot() {
        return root;
    }

    public Node getNode(LocalDateTime dateTime) {
        DateIterator it = new DateIterator(dateTime);
        Node node = root;
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
                    Node next = new Node(path.toString(), node.depth+1);
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
