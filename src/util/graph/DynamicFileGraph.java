package util.graph;

import util.FileTools;

import java.io.File;
import java.util.*;

public class DynamicFileGraph extends FileGraph<Date, File> {
    class Node {
        public String name;
        public int depth;
        public List<Node> children;

        public Node(String name, int depth) {
            this.name = name;
            this.depth = depth;
            this.children = null;
        }

        public boolean add(Node n) {
            if(n == null) return false;
            if(children == null) children = new ArrayList<>();
            return children.add(n);
        }

        public Node getChild(String name) {
            if(children == null) return null;

            for(Node c : children) {
                if(c.name.equals(name))
                    return c;
            }

            return null;
        }
    }

    private int THRESHOLD = 500;
    private Node root;
    private Calendar calendar;
    private int[] timeunitFromDepth;

    public DynamicFileGraph(String root) {
        calendar = Calendar.getInstance();
        timeunitFromDepth = new int[] {calendar.YEAR, calendar.MONTH, calendar.DAY_OF_MONTH,
                calendar.HOUR, calendar.MINUTE};
    }

    public void setRoot(String root) {
        File file = new File(root);
        if(!file.isDirectory()) file.mkdirs();

        this.root = new Node(root, 0);
        Set<Node> edges = new HashSet<>();

        for(File f : new File(root).listFiles()) {
            if(f.isDirectory()) {
                edges.add(build(f, 1));
            }
        }
    }

    private Node build(File file, int depth) {
        if(file == null || !file.isDirectory()) return null;

        Node n = new Node(file.getName(), depth);

        for(File child : file.listFiles()) {
            if(child.isDirectory()) {
                n.children.add(build(child, depth+1));
            }
        }

        return n;
    }

    @Override
    public boolean contains(Date date) {
        return true;
    }

    @Override
    public void add(Date date) {

    }

    @Override
    public File get(Date date) {
        calendar.setTime(date);
        StringBuilder absPath = new StringBuilder(root.name), folderName = new StringBuilder();
        Node temp = root;

        while(temp.children != null) {
            folderName.insert(0, "-").insert(0, getTimeunit(temp.depth));
            absPath.append("\\").append(folderName);

            Node next = temp.getChild(folderName.toString());
            if(next == null) {
                next = new Node(folderName.toString(), temp.depth+1);
                temp.add(next);
            }

            temp = next;
        }

        int fileCount = FileTools.countDirectFiles(new File(absPath.toString()));
        if(fileCount > THRESHOLD) reorganize(temp);

        return new File(absPath.toString());
    }

    private String getTimeunit(int depth) {
        if(depth == 0) return "";
        return String.valueOf(calendar.get(timeunitFromDepth[depth-1]));
    }

    private void reorganize(Node node) {

    }
}
