import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;
import resources.GenerateExampleFiles;
import util.consistency.ModelChecker;
import util.graph.FileGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

public class ModelCheckerTest {
    private static final String repoPath = Path.of("test-bin/repoModelCheck").toAbsolutePath().toString();
    private static Configuration config;
    private static FileGraph graph;
    private static ModelChecker checker;

    /**
     * "test-bin/repo/2010/test2.txt",
     * "test-bin/repo/2021/test4.txt",
     * "test-bin/repo/2023/2023_feb/test1.txt",
     * "test-bin/repo/2023/2023_märz/2023_märz_17/test3.txt",
     * "test-bin/repo/2023/2023_märz/2023_märz_21/test0.txt"
     */

    @BeforeAll
    public static void prepare() {
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        Configuration.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();
        config = new Configuration();

        try {
            initRepo.execute(new String[] {"1"}, config);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

        GenerateExampleFiles.generate();

        Organizer organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.allowExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath, repoPath);

        graph = new FileGraph(repoPath);
        checker = new ModelChecker(config);
    }

    @Test
    public void testFileInCorrectFolder() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        stack.push(graph.getRoot());

        while(!stack.isEmpty()) {
            FileGraph.Node node = stack.pop();

            if(node.leaf) {
                File folder = new File(node.path);
                for(File file : folder.listFiles()) {
                    assertTrue(checker.correctFolder(node.path, file));
                }
            } else {
                for(FileGraph.Node children : node.children.values()) {
                    stack.push(children);
                }
            }
        }
    }

    @Test
    public void testFileInFalseFolder() {
        File folder = new File(repoPath, "2009");
        File falseFile = new File(folder, "falseFile.txt");

        try {
            folder.mkdir();
            falseFile.createNewFile();
        } catch(IOException ioe) {
            fail();
        }

        graph.update(graph.getRoot());

        assertFalse(checker.correctFolder(repoPath+File.separator+"2009", falseFile));

        falseFile.delete();
        folder.delete();
        graph.update(graph.getRoot());
    }

    @Test
    public void testUnderThreshold() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        stack.push(graph.getRoot());

        while(!stack.isEmpty()) {
            FileGraph.Node node = stack.pop();

            if(node.leaf) {
                assertTrue(checker.validNumOfFiles(node));
            } else {
                for(FileGraph.Node children : node.children.values()) {
                    stack.push(children);
                }
            }
        }
    }

    @Test
    public void testAboveThreshold() {
        FileGraph.Node node = graph.getNode(LocalDateTime.of(2010, 1, 1, 1, 1));
        File folder = new File(node.path);
        File incorrectFile = new File(folder, "incorrectFile.txt");

        try {
            incorrectFile.createNewFile();
        } catch(IOException ioe) {
            fail();
        }

        graph.update(node);

        assertFalse(checker.validNumOfFiles(node));

        incorrectFile.delete();
        graph.update(node);
    }

    @Test
    public void testValidFolderName() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        FileGraph.Node root = graph.getRoot();
        stack.push(root);

        while(!stack.isEmpty()) {
            FileGraph.Node node = stack.pop();
            String folderName = node.path.equals(root.path) ? "" : node.path.substring(node.path.lastIndexOf(File.separator)+1);
            assertTrue(checker.validFolderName(folderName));

            for(FileGraph.Node children : node.children.values()) {
                stack.push(children);
            }
        }
    }
}
