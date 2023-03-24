import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileGraphTest {
    private static String[] exampleFoldersStr = new String[] {
            "2021",
            "2022",
            "2021/2021_jan",
            "2021/2021_dez",
            "2021/2021_dez/2021_dez_12"
    };

    private static String root = "test-bin/filegraph";

    private static String[] absoluteExamplePaths = new String[5];

    private static FileGraph fileGraph;

    @BeforeAll
    public static void createFolders() {
        File rootDir = new File(root);
        if(!rootDir.exists()) rootDir.mkdirs();

        for(int i = 0; i < exampleFoldersStr.length; i++) {
            String folder = exampleFoldersStr[i];
            File dir = new File(rootDir, folder);
            if(!dir.exists()) dir.mkdir();
            absoluteExamplePaths[i] = dir.getAbsolutePath();
        }

        fileGraph = new FileGraph(root);
    }

    @Test
    public void updateTestCorrectFolders() {
        List<String> paths = new ArrayList<>();
        allNodePaths(fileGraph.getRoot(), paths);

        assertEquals(exampleFoldersStr.length+1, paths.size());

        for(String folder : absoluteExamplePaths) {
            assertTrue(paths.contains(folder));
        }
    }

    public void allNodePaths(FileGraph.FileNode node, List<String> paths) {
        paths.add(node.path);
        for(FileGraph.FileNode next : node.children.values()) allNodePaths(next, paths);
    }

    @Test
    public void correctLeafValuesTest() {
        correctLeaf(fileGraph.getRoot());
    }

    public void correctLeaf(FileGraph.FileNode node) {
        assertEquals(node.children.isEmpty(), node.leaf);
        for(FileGraph.FileNode next : node.children.values()) correctLeaf(next);
    }

    @Test
    public void getNodeTest() {
        // 12/11/2021 20:50
        LocalDateTime dateTime0 = LocalDateTime.of(2021, 12, 12, 20, 50);
        FileGraph.FileNode node_12_11_2021 = fileGraph.getNode(dateTime0);
        assertEquals(absoluteExamplePaths[4], node_12_11_2021.path);

        // 20/02/2021 19:25
        LocalDateTime dateTime1 = LocalDateTime.of(2021, 2, 20, 19, 25);
        FileGraph.FileNode node_02_2021 = fileGraph.getNode(dateTime1);
        assertEquals(absoluteExamplePaths[0] + File.separator + "2021_feb", node_02_2021.path);

        // 05/07/2020 16:34
        LocalDateTime dateTime2 = LocalDateTime.of(2020, 7, 5, 16, 34);
        FileGraph.FileNode node_2020 = fileGraph.getNode(dateTime2);
        assertEquals(Path.of(root).toAbsolutePath() + File.separator + "2020", node_2020.path);
    }
}
