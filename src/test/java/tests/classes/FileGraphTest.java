package tests.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class contains all tests for the FileGraph class and its functionality.
 */
public class FileGraphTest {
    // example file graph structure as folder path strings
    private static String[] exampleFoldersStr = new String[] {
            "2021",
            "2022",
            "2021/2021_jan",
            "2021/2021_dez",
            "2021/2021_dez/2021_dez_12"
    };
    // path to the root folder of the file graph
    private static String root = "test-bin/filegraph";
    // example folders from exampleFolderStr but as absolute string instead. for safety reasons because
    // I don't trust relative paths.
    private static String[] absoluteExamplePaths = new String[5];
    // file graph object
    private static FileGraph fileGraph;

    /**
     * Create the file graph structure by creating all folders in the exampleFoldersStr.
     */
    @BeforeAll
    public static void createFolders() {
        File rootDir = new File(root);
        if(!rootDir.exists()) rootDir.mkdirs();

        for(int i = 0; i < exampleFoldersStr.length; i++) {
            String folder = exampleFoldersStr[i];
            File dir = new File(rootDir, folder);
            if(!dir.exists()) dir.mkdir();
            // save the absolute path separately
            absoluteExamplePaths[i] = dir.getAbsolutePath();
        }

        // create and build the file graph
        fileGraph = FileGraphFactory.get(root);
    }

    /**
     * Test that all the folders in the file graph mirror the actual filesystem structure.
     */
    @Test
    public void updateTestCorrectFolders() {
        List<String> paths = new ArrayList<>();
        allNodePaths(fileGraph.getRoot(), paths);
        // check that the number of folders is the same, length+1 because the root is included
        assertEquals(exampleFoldersStr.length+1, paths.size());
        // every folder needs to be contained in the graph
        for(String folder : absoluteExamplePaths) {
            assertTrue(paths.contains(folder));
        }
    }

    /**
     * This function iterates through the file graph recursively and adds all folders to a list.
     * @param node current node
     * @param paths list with all folder paths
     */
    public void allNodePaths(FileGraph.Node node, List<String> paths) {
        paths.add(node.path);
        for(FileGraph.Node next : node.children.values()) allNodePaths(next, paths);
    }

    /**
     * Tests that leaf nodes are correctly labeled. Only nodes at the bottom should be leafs, inner nodes
     * shouldn't.
     */
    @Test
    public void correctLeafValuesTest() {
        correctLeaf(fileGraph.getRoot());
    }

    /**
     * This function recursively iterates through the file graph and checks that inner nodes, so nodes that
     * have children nodes/subfolders are not leafs.
     * @param node
     */
    public void correctLeaf(FileGraph.Node node) {
        assertEquals(node.children.isEmpty(), node.leaf);
        for(FileGraph.Node next : node.children.values()) correctLeaf(next);
    }

    /**
     * Test that the correct node is returned on three example dates.
     */
    @Test
    public void getNodeTest() {
        // 12/11/2021 20:50
        LocalDateTime dateTime0 = LocalDateTime.of(2021, 12, 12, 20, 50);
        FileGraph.Node node_12_11_2021 = fileGraph.getNode(dateTime0);
        // should return the node with the path "2021/2021_dez/2021_dez_12"
        assertEquals(absoluteExamplePaths[4], node_12_11_2021.path);

        // 20/02/2021 19:25
        LocalDateTime dateTime1 = LocalDateTime.of(2021, 2, 20, 19, 25);
        FileGraph.Node node_02_2021 = fileGraph.getNode(dateTime1);
        // should return the node with the path "2021/2021_feb" because "2021" is not a leaf node
        // the node for this path doesn't exist but there are sibling nodes "2021/2021_jan" and "2021/2021_dez"
        assertEquals(absoluteExamplePaths[0] + File.separator + "2021_feb", node_02_2021.path);

        // 05/07/2020 16:34
        LocalDateTime dateTime2 = LocalDateTime.of(2020, 7, 5, 16, 34);
        FileGraph.Node node_2020 = fileGraph.getNode(dateTime2);
        // a folder for the year "2020" doesn't exist, so the graph should return a new node with a path
        // for "2020"
        assertEquals(Path.of(root).toAbsolutePath() + File.separator + "2020", node_2020.path);
    }
}
