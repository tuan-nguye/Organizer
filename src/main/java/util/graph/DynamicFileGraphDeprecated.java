package util.graph;

import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DynamicFileGraphDeprecated extends FileGraphDeprecated<Date, String> {
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

    private final int THRESHOLD;
    private Node root;
    private Calendar calendar;
    private int[] timeunitFromDepth;
    private StringBuilder errors;

    public DynamicFileGraphDeprecated(int threshold) {
        THRESHOLD = threshold;
        calendar = Calendar.getInstance();
        timeunitFromDepth = new int[] {calendar.YEAR, calendar.MONTH, calendar.DAY_OF_MONTH,
                calendar.HOUR, calendar.MINUTE};
        errors = new StringBuilder();
    }

    public void setRoot(String root) {
        File file = new File(root);
        if(!file.isDirectory()) file.mkdirs();

        this.root = new Node(root, 0);

        for(File f : new File(root).listFiles()) {
            if(f.isDirectory()) {
                this.root.add(build(f, 1));
            }
        }
    }

    private Node build(File file, int depth) {
        if(file == null || !file.isDirectory()) return null;

        Node n = new Node(file.getName(), depth);

        for(File child : file.listFiles()) {
            if(child.isDirectory()) {
                n.add(build(child, depth+1));
            }
        }

        return n;
    }

    @Override
    public boolean contains(Date date) {
        // dynamic graph: all directories exist because they can be created on demand
        return true;
    }

    @Override
    public void add(Date date) {
        // doesnt exist in this implementation
    }

    @Override
    public String get(Date date) {
        errors.setLength(0);
        calendar.setTime(date);
        StringBuilder absPath = new StringBuilder(root.name), folderName = new StringBuilder();
        Node temp = root;

        while(temp.children != null) {
            if(temp.depth > 0) folderName.insert(0, "-");
            folderName.insert(0, getTimeunit(temp.depth+1));
            absPath.append("/").append(folderName);

            Node next = temp.getChild(folderName.toString());
            if(next == null) {
                next = new Node(folderName.toString(), temp.depth+1);
                temp.add(next);
                new File(absPath.toString()).mkdir();
            }

            temp = next;
        }

        int fileCount = FileTools.countDirectFiles(new File(absPath.toString()));
        if(fileCount > THRESHOLD) {
            reorganize(absPath.toString(), temp);
            return get(date);
        }

        return absPath.toString();
    }

    private String getTimeunit(int depth) {
        if(depth == 0) return "";

        int val = calendar.get(timeunitFromDepth[depth-1]);
        return String.valueOf(depth == 2 ? val+1 : val);
    }

    private void reorganize(String absPath, Node node) {
        File file = new File(absPath);
        Path path = file.toPath();/*
        System.out.println(node.name);
        System.out.println(path.toAbsolutePath());*/

        for(File f : file.listFiles()) {
            calendar.setTime(new Date(f.lastModified()));
            String folder = getTimeunit(node.depth+1);
            if(node.depth > 0) folder += "-" + node.name;
            Path dest = path.resolve(folder);

            if(Files.notExists(dest)) {
                node.add(new Node(folder, node.depth+1));
                dest.toFile().mkdir();
            }

            move(f.toPath(), dest.resolve(f.getName()));
        }
    }

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException ie) {}
    }

    private void move(Path source, Path dest) {
        try {
            Files.move(source, dest);
        } catch(IOException ioe) {
            errors.append("error when moving ").append(source).append("\n");
            ioe.printStackTrace();
        }
    }

    public String getErrors() {
        return errors.toString();
    }

    public String print(Node node) {
        StringBuilder out = new StringBuilder(", " + node.name);
        if(node.children == null) return out.toString();

        out.append("[");
        for(Node nn : node.children)
            out.append(print(nn));

        return out.append("]").toString();
    }

    public static void main(String[] args) {
        DynamicFileGraphDeprecated g = new DynamicFileGraphDeprecated(10);
        g.setRoot("C:\\Users\\User\\Documents\\test");
        System.out.println(g.print(g.root));

        Date now = new Date(System.currentTimeMillis());

        System.out.println(now);
        String str1 = g.get(now);
        System.out.println(str1);
    }
}
