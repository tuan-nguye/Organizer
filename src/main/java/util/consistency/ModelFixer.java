package util.consistency;

import organizer.ThresholdOrganizer;
import organizer.copy.ICopy;
import organizer.copy.Move;
import parser.Configuration;
import util.FileTools;
import util.graph.FileGraph;
import util.graph.FileGraphFactory;
import util.time.DateIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class ModelFixer {
    private FileGraph fileGraph;
    private ModelChecker checker;
    private int threshold;

    public ModelFixer(Configuration config) {
        fileGraph = FileGraphFactory.getFileGraph(Configuration.PROPERTY_FILE_PATH_STRING);
        checker = new ModelChecker(config);
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    public void fixStructure(Map<ModelError, List<ModelElement>> errors, boolean fixFiles, boolean fixFolders) {
        //fileGraph.update(fileGraph.getRoot());
        // fix structure by attempting to restore the original folder name
        Set<FileGraph.Node> unrestorableFolders = fixFolders(errors.get(ModelError.INVALID_FOLDER_STRUCTURE), errors.get(ModelError.INVALID_FOLDER_NAME));
        // union of invalid folder name, above threshold, and files in non leaf folder
        // move all of them to (tmp directory/directly into structure)
        for(ModelElement me : errors.get(ModelError.FOLDER_ABOVE_THRESHOLD)) unrestorableFolders.add(me.node);
        for(ModelElement me : errors.get(ModelError.FILES_IN_NON_LEAF)) unrestorableFolders.add(me.node);
        ThresholdOrganizer org = new ThresholdOrganizer(new Move(), threshold);
        for(FileGraph.Node invalidFolder : unrestorableFolders) org.copyAndOrganize(invalidFolder.path, fileGraph.getRoot().path, true);
        // last is moving all single files that are in wrong folder into correct folder
        for(ModelElement me : errors.get(ModelError.FILE_IN_WRONG_FOLDER)) org.copyAndOrganize(me.toString(), fileGraph.getRoot().path, true);
        reduceStructure();
    }

    private Set<FileGraph.Node> fixFolders(List<ModelElement> invalidStructure, List<ModelElement> invalidFolderNames) {
        Set<FileGraph.Node> invalidFolderNamesSet = new HashSet<>();
        for(ModelElement me : invalidFolderNames) invalidFolderNamesSet.add(me.node);
        Set<FileGraph.Node> unrestorableFolders = new HashSet<>();

        for(ModelElement me : invalidStructure) {
            String original = me.toString();
            String[] folders = me.toString().substring(fileGraph.getRoot().path.length()+1).split(Pattern.quote(File.separator));
            List<FileGraph.Node> path = new ArrayList<>();
            FileGraph.Node node = fileGraph.getRoot();
            StringBuilder key = new StringBuilder(node.path);
            path.add(node);
            for(int i = 0; i < folders.length; i++) {
                key.append(File.separator).append(folders[i]);
                node = node.children.get(key.toString());
                path.add(node);
            }

            // start with leaf node, if valid name: do nothing
            // else: iterate through all files, if all from same date: restore folder name, else: mark as unrestorable
            boolean restored = false;
            File leafFolder = new File(me.node.path);
            StringBuilder folderNameBuilder = new StringBuilder();
            File[] files = leafFolder.listFiles();
            if(files.length == 0) {
                for(FileGraph.Node np : path) unrestorableFolders.add(np);
                continue;
            }
            if(files == null) throw new NullPointerException("listFiles() of leaf folder is null");
            DateIterator di = new DateIterator(FileTools.dateTime(files[0].lastModified()));
            boolean first = true;
            for(int i = 0; i < me.node.depth; i++) {
                if(first) first = false;
                else folderNameBuilder.append("_");
                folderNameBuilder.append(di.next());
            }
            String folderName = folderNameBuilder.toString();
            boolean same = true;
            for(File f : files) {
                if(!correctFolder(folderName, f)) {
                    same = false;
                    break;
                }
            }
            if(!same) {
                for(FileGraph.Node np : path) unrestorableFolders.add(np);
                continue;
            }

            if(!leafFolder.getName().equals(folderName)) {
                if(!leafFolder.renameTo(new File(leafFolder.getParentFile(), folderName))) {
                    for(FileGraph.Node np : path) unrestorableFolders.add(np);
                    continue;
                } else {
                    path.get(path.size()-1).path = path.get(path.size()-2).path + File.separator + folderName;
                    restored = true;
                }
            }

            // complete the rest
            FileGraph.Node prevNode = me.node;
            for(int i = path.size()-2; i >= 1; i--) {
                FileGraph.Node currNode = path.get(i);
                File currFolder = new File(currNode.path);
                String newFolderName = FileTools.getFolderNameWithoutPrefix(fileGraph.getRoot().path, prevNode.path);
                int idxUnderscore = newFolderName.lastIndexOf("_");
                if(idxUnderscore != -1) {
                    newFolderName = newFolderName.substring(0, idxUnderscore);
                } else throw new IllegalStateException("should not rename root folder");

                if(newFolderName.equals(currFolder.getName())) {
                    continue;
                } else if(!currFolder.renameTo(new File(currFolder.getParentFile(), newFolderName))) {
                    for(FileGraph.Node np : path) unrestorableFolders.add(np);
                    break;
                } else {
                    currNode.path = path.get(i-1).path + File.separator + newFolderName;
                    restored = true;
                }
                prevNode = currNode;
            }
            if(restored) {
                updateFolders();
                System.out.printf("successfully restored structure:\n%s -> %s\n", original, me.toString());
            }

        }

        return unrestorableFolders;
    }

    private void updateFolders() {
        FileGraph.Node root = fileGraph.getRoot();
        updateFolders(root, new StringBuilder(root.path));
    }

    private void updateFolders(FileGraph.Node node, StringBuilder path) {
        String folderName = FileTools.getFolderNameWithoutPrefix(fileGraph.getRoot().path, node.path);
        int originalLength = path.length();
        if(!folderName.isEmpty()) path.append(File.separator).append(folderName);
        node.path = path.toString();

        if(!node.leaf) {
            List<Map.Entry<String, FileGraph.Node>> entries = new ArrayList<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                updateFolders(child, path);
                String key = e.getKey();
                node.children.remove(key);
                node.children.put(child.path, child);
            }
        }

        // restore old state
        path.setLength(originalLength);
    }

    private boolean correctFolder(String folderName, File f) {
        DateIterator it = new DateIterator(FileTools.dateTime(f.lastModified()));
        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }


    /**
     * reduce the structure if it shouldnt be split
     * if the number of files in its children is not above the threshold
     * then it shouldn't be split, so copy all files in the child folders
     * into itself then delete children
     * dfs post order
     */
    public void reduceStructure() {
        fileGraph.update(fileGraph.getRoot());
        reduceStructure(fileGraph.getRoot());
    }

    private void reduceStructure(FileGraph.Node node) {
        if(node.leaf) {
            // nothing to do
        } else {
            int numFiles = 0;
            boolean allLeaves = true;

            Set<Map.Entry<String, FileGraph.Node>> entries = new HashSet<>(node.children.entrySet());
            for(Map.Entry<String, FileGraph.Node> e : entries) {
                FileGraph.Node child = e.getValue();
                reduceStructure(child);
                numFiles += child.fileCount;
                allLeaves &= child.leaf;
                if(child.leaf && child.fileCount == 0) {
                    String key = e.getKey();
                    node.children.remove(key);
                    File emptyChild = new File(child.path);
                    if(!emptyChild.delete()) throw new IllegalStateException("couldnt delete empty child: " + child.path);
                }
            }

            if(allLeaves && numFiles <= threshold) {
                ICopy moveOp = new Move();
                Path currDir = Path.of(node.path);
                for(FileGraph.Node child : node.children.values()) {
                    File childFolder = new File(child.path);
                    for(File f : childFolder.listFiles()) {
                        try {
                            moveOp.execute(f.toPath(), currDir.resolve(f.getName()));
                        } catch(IOException ioe) {
                            System.err.println("error moving during restructuring: " + f.getAbsolutePath());
                            ioe.printStackTrace();
                        }
                    }
                    if(!childFolder.delete()) throw new IllegalStateException("cant delete folder after moving all files: " + child.path);

                }

                node.children.clear();
                node.fileCount = numFiles;
            }

            if(node.children.isEmpty()) node.leaf = true;
        }
    }
}
