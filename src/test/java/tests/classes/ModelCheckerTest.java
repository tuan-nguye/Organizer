package tests.classes;

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
import resources.InitializeTestRepository;
import util.FileTools;
import util.consistency.ModelChecker;
import util.consistency.ModelError;
import util.graph.FileGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

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
        config = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, config);

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
            String folderName = FileTools.getFolderNameWithoutPrefix(root.path, node.path);
            assertTrue(checker.validFolderName(folderName));

            for(FileGraph.Node children : node.children.values()) {
                stack.push(children);
            }
        }
    }

    @Test
    public void testValidFolderStructure() {
        testValidFolderStructureRec(graph.getRoot(), new ArrayList<>());
    }

    private void testValidFolderStructureRec(FileGraph.Node node, List<String> folders) {
        String folderName = FileTools.getFolderNameWithoutPrefix(graph.getRoot().path, node.path);
        folders.add(folderName);

        if(!node.leaf) {
            for(FileGraph.Node next : node.children.values()) {
                testValidFolderStructureRec(next, folders);
            }
        } else {
            System.out.println(folders);
            assertTrue(checker.validFolderStructure(folders));
        }

        folders.remove(folders.size()-1);
    }

    @Test
    public void testValidFolderStructureFalse() {
        List<String> folders = List.of("", "2010", "2010_asdf");
        assertFalse(checker.validFolderStructure(folders));
    }

    @Test
    public void checkAllCorrect() {
        checker.checkAll(true, true);
        String out = "";
        for(Map.Entry<ModelError, List<String>> e : checker.getErrors().entrySet()) {
            if(!e.getValue().isEmpty()) out += e + "\n";
        }

        if(!out.isEmpty()) fail("errors detected even though everything is correct: \n" + out);
    }

    @Test
    public void checkAllTestIncorrectSingle() {
        // TODO add invalid folders and files
        // TODO checker should find all of them
        // TODO and then remove them again
        // file in wrong folder
        File fileWrongFolder = new File(repoPath+File.separator+"1970", "wrong_folder.txt");

        try {
            new File(repoPath+File.separator+"1970").mkdirs();
            fileWrongFolder.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // incorrect folder name
        File folderInvalidName = new File(repoPath+File.separator+"el_wiwi");
        try {
            folderInvalidName.mkdirs();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // number of files above threshold
        long lm = 1079387493013l;
        LocalDateTime ldt = FileTools.dateTime(lm);
        FileGraph.Node node = graph.getNode(ldt);
        File folderAboveThreshold = new File(node.path);
        try {
            folderAboveThreshold.mkdirs();
            File f0 = new File(folderAboveThreshold, "above_thresh0.txt");
            f0.createNewFile();
            f0.setLastModified(lm);
            File f1 = new File(folderAboveThreshold, "above_thresh1.txt");
            f1.createNewFile();
            f1.setLastModified(lm);
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // files in non leaf node
        File folderNonLeaf = new File(repoPath+File.separator+"2023");
        File fileNonLeaf = new File(folderNonLeaf, "file_inner_node.txt");
        try {
            fileNonLeaf.createNewFile();
            fileNonLeaf.setLastModified(1675387890214l);
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // update graph
        graph.update(graph.getRoot());

        checker.checkAll(true, true);

        // check if all errors have been found
        Map<ModelError, List<String>> errors = checker.getErrors();

        for(ModelError me : ModelError.values()) {
            if(errors.get(me).size() != 1) fail(me + " not found");
        }

        assertEquals(errors.get(ModelError.FILE_IN_WRONG_FOLDER).get(0), fileWrongFolder.getAbsolutePath());
        assertEquals(errors.get(ModelError.INVALID_FOLDER_NAME).get(0), folderInvalidName.getAbsolutePath());
        assertEquals(errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).get(0), folderAboveThreshold.getAbsolutePath());
        assertEquals(errors.get(ModelError.FILES_IN_NON_LEAF).get(0), folderNonLeaf.getAbsolutePath());
        assertEquals(errors.get(ModelError.INVALID_FOLDER_STRUCTURE).get(0), folderInvalidName.getAbsolutePath());

        FileTools.delete(fileWrongFolder.getParentFile());
        FileTools.delete(folderInvalidName);
        FileTools.delete(folderAboveThreshold);
        FileTools.delete(fileNonLeaf);
    }

    @Test
    public void testCheckAllIncorrectMultiple() {
        File folderInvalidNameAboveThreshold = new File(repoPath+File.separator+"el_wiwi");
        File f0 = new File(folderInvalidNameAboveThreshold, "el_wiwi0.png");
        File f1 = new File(folderInvalidNameAboveThreshold, "el_wiwi1.png");

        try {
            folderInvalidNameAboveThreshold.mkdirs();
            f0.createNewFile();
            f1.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        checker.checkAll(true, true);

        int numErr = 0;
        for(Map.Entry<ModelError, List<String>> e : checker.getErrors().entrySet()) {
            numErr += e.getValue().size();
        }
        assertEquals(5, numErr);

        FileTools.delete(folderInvalidNameAboveThreshold);
    }
}
