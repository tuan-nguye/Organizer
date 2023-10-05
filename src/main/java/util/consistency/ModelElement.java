package util.consistency;

import util.graph.FileGraph;

import java.io.File;

// model element to save where errors are located in the filegraph
public class ModelElement {
    // location
    public FileGraph.Node node;
    // file name if it exists, folders have a blank string instead
    public String fileName;

    public ModelElement(FileGraph.Node node) {
        this(node, "");
    }

    public ModelElement(FileGraph.Node node, String fileName) {
        this.node = node;
        this.fileName = fileName;
    }

    public String toString() {
        return fileName.isEmpty() ? node.path : node.path + File.separator + fileName;
    }
}
