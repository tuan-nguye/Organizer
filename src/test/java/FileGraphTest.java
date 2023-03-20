import org.junit.jupiter.api.Test;
import util.graph.FileGraph;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileGraphTest {

    @Test
    public void test() {
        String path = "src/test/resources";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        System.out.println(absolutePath);
        FileGraph fileGraph = new FileGraph(absolutePath);
        FileGraph.FileNode root = fileGraph.getRoot();
        System.out.println(root.path);
        fileGraph.printFileStructure();
    }
}
