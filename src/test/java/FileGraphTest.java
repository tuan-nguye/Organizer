import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileGraphTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();

    @BeforeAll
    public static void prepare() {
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        Configuration.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();
        Configuration conf = new Configuration();
        try {
            initRepo.execute(new String[] {String.valueOf(2)}, conf);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

    }

    @Test
    public void test() {
        File file = new File(repoPath);
        String absolutePath = file.getAbsolutePath();
        System.out.println(absolutePath);
        FileGraph fileGraph = new FileGraph(absolutePath);
        FileGraph.FileNode root = fileGraph.getRoot();
        System.out.println(root.path);
        fileGraph.printFileStructure();
    }
}
