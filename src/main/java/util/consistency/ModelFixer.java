package util.consistency;

import organizer.ThresholdOrganizer;
import organizer.copy.ICopy;
import organizer.copy.Move;
import parser.Configuration;
import util.time.DateExtractor;
import util.time.DateTools;
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
        fileGraph = FileGraphFactory.get(Configuration.PROPERTY_FILE_PATH_STRING);
        checker = new ModelChecker(config);
        threshold = Integer.parseInt(config.getProperties().getProperty("folderSize"));
    }

    public void fixStructure(Map<ModelError, List<FileGraph.Node>> errors, boolean fixFiles, boolean fixFolders) {
        if(!fixFiles && !fixFolders) return;
        //fileGraph.update(fileGraph.getRoot());
        // fix structure by attempting to restore the original folder name
        Set<FileGraph.Node> unrestorableFolders;
        if(fixFolders) {
            unrestorableFolders = fixFolders(errors);
        } else {
            unrestorableFolders = new HashSet<>(errors.get(ModelError.INVALID_FOLDER_STRUCTURE));
            unrestorableFolders.addAll(errors.get(ModelError.INVALID_FOLDER_NAME));
            unrestorableFolders.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));
        }
        // union of invalid folder name, above threshold, and files in non leaf folder
        // move all of them to (tmp directory/directly into structure)
        unrestorableFolders.addAll(errors.get(ModelError.FILES_IN_NON_LEAF));
        Set<FileGraph.Node> foldersAboveThreshold = new HashSet<>();
        for(FileGraph.Node invalidFolder : unrestorableFolders) {
            copyFilesToCorrectLocation(invalidFolder, foldersAboveThreshold);
            if(invalidFolder.leaf) invalidFolder.fileCount = FileTools.countDirectFiles(new File(invalidFolder.path));
            else invalidFolder.fileCount = 0;
        }
        ThresholdOrganizer org = new ThresholdOrganizer(new Move(), threshold, fileGraph.getRoot().path);
        foldersAboveThreshold.addAll(errors.get(ModelError.FOLDER_ABOVE_THRESHOLD));
        for(FileGraph.Node folder : foldersAboveThreshold) {
            if(folder.fileCount > threshold) org.reorganize(folder);
        }
        reduceStructure();
    }

    private Set<FileGraph.Node> fixFolders(Map<ModelError, List<FileGraph.Node>> errors) {
        // put all faulty folders in one set
        Set<FileGraph.Node> faultyFolders = new HashSet<>();
        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_STRUCTURE)) {
            List<FileGraph.Node> path = getPathToNode(fn);
            faultyFolders.addAll(path);
        }

        faultyFolders.addAll(errors.get(ModelError.INVALID_FOLDER_NAME));
        faultyFolders.addAll(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES));

        // restore

        for(FileGraph.Node fn : errors.get(ModelError.INVALID_FOLDER_STRUCTURE)) {
            String original = fn.path;
            List<FileGraph.Node> path = getPathToNode(fn);
            boolean restored = restoreFolder(fn, path, faultyFolders);

            if(restored) updateFolders();
            if(!fn.path.equals(original)) {
                updateFolders();
                path = getPathToNode(fn);
                faultyFolders.removeAll(path);
                System.out.printf("successfully restored structure:\n%s -> %s\n", original, fn.path);
            } else {
                faultyFolders.addAll(path);
            }
        }

        return faultyFolders;
    }

    public boolean restoreFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        if(!restoreLeafFolder(node, path, faultyFolders)) return false;
        if(!restoreInnerFolders(node, path, faultyFolders)) return false;
        return true;
    }

    private boolean restoreLeafFolder(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        if(!allAreSameDate(node)) return false;

        File leafFolder = new File(node.path);
        File[] files = leafFolder.listFiles(a -> a.isFile());
        if(files.length == 0) throw new IllegalStateException("folder size is 0");
        String folderName = DateTools.folderName(files[0], node.depth);


        if(!leafFolder.getName().equals(folderName)) {
            // check sibling nodes
            if(path.size() >= 3 && !validateWithSiblings(path.get(path.size()-2), node, faultyFolders)) {
                return false;
            }

            // attempt rename
            if(!renameFolder(node, folderName)) return false;
        }

        return true;
    }

    private boolean restoreInnerFolders(FileGraph.Node node, List<FileGraph.Node> path, Set<FileGraph.Node> faultyFolders) {
        FileGraph.Node nextNode = node;

        for(int i = path.size()-2; i >= 1; i--) {
            FileGraph.Node currNode = path.get(i);
            File currFolder = new File(currNode.path);
            String newFolderName = correctPreviousFolderName(nextNode.path);

            if(!newFolderName.equals(currFolder.getName())) {
                if(!validateWithSiblings(path.get(i-1), currNode, faultyFolders)) {
                    return false;
                }

                if(!renameFolder(currNode, newFolderName)) return false;
            }

            nextNode = currNode;
        }

        return true;
    }

    /**
     *
     * @param node
     * @return returns the path from root to given node in a list
     */
    private List<FileGraph.Node> getPathToNode(FileGraph.Node node) {
        String[] folders = node.path.substring(fileGraph.getRoot().path.length()+1).split(Pattern.quote(File.separator));
        List<FileGraph.Node> path = new ArrayList<>();
        FileGraph.Node temp = fileGraph.getRoot();
        StringBuilder key = new StringBuilder(temp.path);
        path.add(temp);
        for(int i = 0; i < folders.length; i++) {
            key.append(File.separator).append(folders[i]);
            temp = temp.children.get(key.toString());
            path.add(temp);
        }

        return path;
    }

    /**
     *
     * @param node
     * @return true if there are files in the folder, and they are
     * all from the same date until the node's depth
     */
    private boolean allAreSameDate(FileGraph.Node node) {
        File folder = new File(node.path);
        StringBuilder folderNameBuilder = new StringBuilder();
        File[] files = folder.listFiles(a -> a.isFile());

        if(files == null || files.length == 0) return false;

        DateIterator di = new DateIterator(DateExtractor.getDate(files[0]));
        boolean first = true;
        for(int i = 0; i < node.depth; i++) {
            if(first) first = false;
            else folderNameBuilder.append("_");
            folderNameBuilder.append(di.next());
        }
        String folderName = folderNameBuilder.toString();

        for(File f : files) {
            if(!correctFolder(folderName, f)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param parent
     * @param node
     * @param faultyNodes
     * @return true if the new folder name is conform with the other
     * valid sibling nodes
     */
    private boolean  validateWithSiblings(FileGraph.Node parent, FileGraph.Node node, Set<FileGraph.Node> faultyNodes) {
        if(node.depth <= 1) return true;
        String folderName = DateTools.folderName(new File(node.path).listFiles(a -> a.isFile())[0], node.depth);

        String prefix = folderName.substring(0, folderName.lastIndexOf('_'));
        for(FileGraph.Node sibling : parent.children.values()) {
            if(faultyNodes.contains(sibling)) continue;
            String siblingFolder = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, sibling.path);
            if(!siblingFolder.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param folderPath
     * @return the correct folder given the next folder name, e.g.
     * if folderName = "2008_jan" then the correct previous folder
     * is "2008"
     */
    private String correctPreviousFolderName(String folderPath) {
        String folderName = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, folderPath);
        int idxUnderscore = folderName.lastIndexOf("_");
        if(idxUnderscore != -1) folderName = folderName.substring(0, idxUnderscore);
        else folderName = "";

        return folderName;
    }

    private boolean renameFolder(FileGraph.Node node, String newFolderName) {
        File folder = new File(node.path);
        File renamedFolder = new File(folder.getParent(), newFolderName);
        if(!folder.renameTo(renamedFolder)) {
            return false;
        }

        node.path = renamedFolder.getAbsolutePath();
        return true;
    }

    private void updateFolders() {
        FileGraph.Node root = fileGraph.getRoot();
        updateFolders(root, new StringBuilder(root.path));
    }

    private void updateFolders(FileGraph.Node node, StringBuilder path) {
        String folderName = FileTools.getNameWithoutPrefix(fileGraph.getRoot().path, node.path);
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
        DateIterator it = new DateIterator(DateExtractor.getDate(f));
        String[] folderSplit = folderName.split("_");

        for(int i = 0; i < folderSplit.length; i++) {
            String split = folderSplit[i];
            String dateStr = it.next();
            if(!split.equals(dateStr)) return false;
        }

        return true;
    }

    private void copyFilesToCorrectLocation(FileGraph.Node folderNode, Set<FileGraph.Node> foldersAboveThreshold) {
        File folder = new File(folderNode.path);
        ICopy move = new Move();

        for(File file : folder.listFiles(f -> f.isFile())) {
            if(file.getName().equals(Configuration.PROPERTY_FILE_NAME_STRING)) continue;
            FileGraph.Node correctNode = fileGraph.getNode(DateExtractor.getDate(file));
            File correctFolder = new File(correctNode.path);
            if(!correctFolder.exists()) correctFolder.mkdir();
            Path from = file.toPath(), to = Path.of(correctNode.path, file.getName());
            if(from.equals(to)) continue;

            try {
                move.execute(file.toPath(), Path.of(correctNode.path, file.getName()));
                correctNode.fileCount++;
                if(correctNode.fileCount > threshold) foldersAboveThreshold.add(correctNode);
            } catch(IOException ioe) {
                System.out.println("modelfixer: error when moving file to correct location");
                ioe.printStackTrace();
            }
        }
    }

    /**
     * reduce the structure if it shouldnt be split
     * if the number of files in its children is not above the threshold
     * then it shouldn't be split, so copy all files in the child folders
     * into itself then delete children
     * dfs post order
     */
    public void reduceStructure() {
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
