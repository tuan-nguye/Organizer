package util.graph;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileDirectoryGraphDeprecated extends FileGraphDeprecated<String[], File> {
    private File root;
    private Path pathToRoot;
    private Map<File, Map<String, File>> adj;

    public FileDirectoryGraphDeprecated() {
        adj = new HashMap<>();
    }

    public void setRoot(String root) {
        this.root = new File(root);
        if(!this.root.isDirectory()) this.root.mkdirs();
        pathToRoot = this.root.toPath();
        adj.clear();

        build(this.root);
        //System.out.println(adj);
    }

    private void build(File file) {
        if(file == null || file.isFile()) return;

        Map<String, File> edges;
        if((edges = adj.get(file)) == null) {
            edges = new HashMap<>();
            adj.put(file, edges);
        }

        for(File child : file.listFiles()) {
            if(child.isDirectory()) {
                edges.put(child.getName(), child);
                build(child);
            }
        }
    }

    public boolean contains(String[] path) {
        File temp = root;

        for(String folder : path) {
            Map<String, File> edges = adj.get(temp);
            if(edges == null) return false;
            temp = edges.get(folder);
            if(temp == null) return false;
        }

        return true;
    }

    public void add(String[] path) {
        File temp = root;
        StringBuilder completePath = new StringBuilder(temp.getAbsolutePath());

        for(String folder : path) {
            completePath.append("\\").append(folder);
            if(!adj.containsKey(temp)) adj.put(temp, new HashMap<>());
            Map<String, File> edges = adj.get(temp);

            if(!edges.containsKey(folder)) {
                File newFile = new File(completePath.toString());
                newFile.mkdir();
                edges.put(folder, newFile);
            }

            temp = edges.get(folder);
        }
        //System.out.println(adj);
    }

    public File get(String[] path) {
        if(!contains(path)) return null;
        File temp = root;

        for(String folder : path) {
            temp = adj.get(temp).get(folder);
        }

        return temp;
    }

    @Override
    public String getErrors() {
        return null;
    }

    public static void main(String[] args) {
        String root = "C:/Users/User/Documents/bios_mod";
        FileDirectoryGraphDeprecated graph = new FileDirectoryGraphDeprecated();
        graph.setRoot(root);
    }
}
