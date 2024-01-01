package com.org.util.graph;

import com.org.parser.Configuration;
import com.org.util.time.DateIterator;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that keeps a minimal model of the directory structure to a root path. Information
 * like the number of direct files in the folder, if it's a leaf, folder depth from the
 * root, etc. are stored. This saves IO operations because they don't need to extracted
 * by using the Java file API. The graph consists of nodes where each node represents a
 * folder.
 */
public class FileGraph {
    /**
     * Node class for the graph structure. Each node stores one absolute path to a folder.
     */
    public static class Node {
        // absoulte path to the folder
        public String path;
        // number of direct files (not directories)
        public int fileCount = 0;
        // total number of files in this folder and all subfolders
        public int fileCountSubTree = 0;
        // total size in bytes in this folder and all subfolders
        public long sizeTotal = 0;
        // depth in the graph/distance from the root
        public int depth;
        // boolean value indicating if the node is a leaf node or not
        public boolean leaf = true;
        // map of all children/subfolders, maps the absolute path to the child
        // folder to its node
        public Map<String, Node> children = new HashMap<>();

        /**
         * Node constructor
         * @param path absolute path to the folder
         * @param depth depth of the node inside the whole graph
         */
        public Node(String path, int depth) {
            this.path = path;
            this.depth = depth;
        }

        /**
         * Return the node as formatted string.
         * @return string
         */
        @Override
        public String toString() {
            return String.format("%s, files: %d, filesTotal: %d, size: %d, leaf: %b", path, fileCount, fileCountSubTree, sizeTotal, leaf);
        }
    }

    // reference to the root node
    Node root;

    /**
     * FileGraph constructor. Builds the graph while iterating through all files and folders.
     * @param rootStr string path to the root folder
     */
    public FileGraph(String rootStr) {
        root = new Node(Path.of(rootStr).toAbsolutePath().toString(), 0);
        update(root);
    }

    /**
     * Update the file graph starting at the given node and its subtree. This function
     * accesses the folders associated to the nodes and updates file count and other
     * fields. After the recursive function finishes, the model of the files are
     * identical to the filesystem.
     * @param node
     */
    public void update(Node node) {
        File file = new File(node.path);
        // the file must exist and be a directory
        if(!file.exists() || !file.isDirectory()) {
            node.children.clear();
            return;
        }
        // prepare field values
        int fileCount = 0;
        int fileCountSubTree = 0;
        long size = 0;
        // store nodes that don't exist anymore, because the folder is gone
        Set<String> toRemove = new HashSet<>(node.children.keySet());

        for(File child : file.listFiles()) {
            if(child.isFile()) {
                // skip the hidden configuration file
                if(child.isHidden() && child.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
                // increment the file count and add to the total size
                fileCount++;
                size += child.length();
            } else {
                // if the child folder doesn't exist yet, create a new entry for the child
                String nextStr = child.getAbsolutePath();
                if(!node.children.containsKey(nextStr)) node.children.put(nextStr, new Node(nextStr, node.depth+1));
                Node nextNode = node.children.get(nextStr);
                // mark the child path as not to be removed
                toRemove.remove(nextStr);
                update(nextNode);
                // if the child is a folder, add to its subtree total count
                fileCountSubTree += nextNode.fileCountSubTree;
                size += nextNode.sizeTotal;
            }
        }

        // all nodes whose folders don't exist in the filesystem anymore, need to be removed
        for(String rm : toRemove) node.children.remove(rm);
        // update the node's fields with the new values
        node.fileCount = fileCount;
        node.leaf = node.children.isEmpty();
        node.fileCountSubTree = fileCountSubTree + fileCount;
        node.sizeTotal = size;
    }

    /**
     * This function prints the file graph's structure into the console.
     * Indentations indicate the node's depth.
     */
    public void printFileStructure() {
        printFileStructure(root, 0);
    }

    /**
     * This function implements the printFileStructure() recursive functionality. It
     * iterates through all nodes and subnodes and adds indentation depending on the
     * node's depth within the file graph tree.
     * @param node current visited node
     * @param depth depth of the node inside the tree
     */
    private void printFileStructure(Node node, int depth) {
        System.out.println("\t".repeat(depth) + node);
        for(Node child : node.children.values()) {
            printFileStructure(child, depth+1);
        }
    }

    /**
     * Get the file graph's root node.
     * @return root node
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Returns the leaf node for the given datetime. The function searches through
     * the tree to find the correct leaf node according to the year, month, day of
     * month, etc. The path of the returned node might not exist yet, so it needs
     * to be created first.
     * @param dateTime date time object
     * @return the leaf node
     */
    public Node getNode(LocalDateTime dateTime) {
        DateIterator it = new DateIterator(dateTime);
        Node node = root;
        // stores the current absolute path as string
        StringBuilder path = new StringBuilder(root.path);
        // stores the current folder name as string
        StringBuilder folderName = new StringBuilder();
        boolean first = true;

        // iterate through the tree starting from the root and stop if a leaf
        // node is reached
        while(it.hasNext()) {
            if(first) first = false;
            else folderName.append("_");
            folderName.append(it.next());
            path.append(File.separator).append(folderName);
            // reached the end if the folder is not in the children map of the node
            if(!node.children.containsKey(path.toString())) {
                if(!node.leaf) {
                    // if the node is not a leaf node but an inner node, the folder
                    // was not yet created. A new node will be created
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
