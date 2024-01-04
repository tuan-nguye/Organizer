package tests.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.Copy;
import com.org.parser.Configuration;
import tests.resources.GenerateExampleFiles;
import tests.resources.InitializeTestRepository;
import com.org.util.FileTools;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelError;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains all tests for the class ModelChecker.
 */
public class ModelCheckerTest {
    // path to the repository root to perform the tests
    private static final String repoPath = Path.of("test-bin/repoModelCheck").toAbsolutePath().toString();
    // configuration object needed for the command objects
    private static Configuration config;
    // file graph object
    private static FileGraph graph;
    // model checker object
    private static ModelChecker checker;
    // maximum folder size threshold
    private static int threshold = 1;
    // node reference to the error node
    private static FileGraph.Node errorNode;


    /**
     * Prepares all objects and sets them to their initial state.
     *
     * structure of the initial state of the file graph with threshold = 1
     * "test-bin/repo/2010/test2.txt",
     * "test-bin/repo/2021/test4.txt",
     * "test-bin/repo/2023/2023_feb/test1.txt",
     * "test-bin/repo/2023/2023_märz/2023_märz_17/test3.txt",
     * "test-bin/repo/2023/2023_märz/2023_märz_21/test0.txt"
     */
    @BeforeAll
    public static void prepare() {
        config = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, config, threshold);

        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold, repoPath);
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath);

        graph = FileGraphFactory.get(repoPath);
        checker = new ModelChecker(config);
        FileGraph.Node root = graph.getRoot();
        errorNode = root.children.get(root.path + File.separator + Configuration.ERROR_FOLDER_NAME);
    }

    /**
     * All files are in their correct location. This test makes sure that the modelchecker function correctFolder()
     * returns true on all folders by recursively iterating through the file graph.
     */
    @Test
    public void testFileInCorrectFolder() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        stack.push(graph.getRoot());

        while(!stack.isEmpty()) {
            FileGraph.Node node = stack.pop();

            if(node.leaf) {
                File folder = new File(node.path);
                for(File file : folder.listFiles()) {
                    assertTrue(checker.correctFolder(node, file));
                }
            } else {
                for(FileGraph.Node children : node.children.values()) {
                    stack.push(children);
                }
            }
        }
    }

    /**
     * This test intentionally creates a folder in the wrong directory. The function correctFolder() should return
     * false on that file when called.
     */
    @Test
    public void testFileInWrongFolder() {
        File folder = new File(repoPath, "2009");
        // newly creates files have the current date as lastModified field and its way past 2009
        File falseFile = new File(folder, "falseFile.txt");

        try {
            folder.mkdir();
            falseFile.createNewFile();
        } catch(IOException ioe) {
            fail();
        }
        // update the graph after creating a new file and folder
        graph.update(graph.getRoot());
        // get the node for the folder in 2009
        FileGraph.Node node = graph.getNode(LocalDateTime.of(2009, 1, 1, 1, 1));
        assertFalse(checker.correctFolder(node, falseFile));
        // reset the repo to its original state
        falseFile.delete();
        folder.delete();
        graph.update(graph.getRoot());
    }

    /**
     * The function validNumOfFiles() should always return true on every node if the file graph structure is correct,
     * e.g. every folder has fewer files than the threshold=1.
     */
    @Test
    public void testUnderThreshold() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        stack.push(graph.getRoot());
        // recursively iterate through the file graph with a stack
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

    /**
     * Create a new file in a folder so that the threshold for that folder is exceeded. The checker should be able
     * to detect that with validNumOfFiles().
     */
    @Test
    public void testAboveThreshold() {
        // get the 2010 node which already contains one file so: folder size == threshold
        FileGraph.Node node = graph.getNode(LocalDateTime.of(2010, 1, 1, 1, 1));
        // create a new file in that folder, now the folder size is two > threshold
        File folder = new File(node.path);
        File incorrectFile = new File(folder, "incorrectFile.txt");

        try {
            incorrectFile.createNewFile();
        } catch(IOException ioe) {
            fail();
        }
        // update the file graph after creating a new file, also updates the file count for every node
        graph.update(node);
        // the node contains too many files now
        assertFalse(checker.validNumOfFiles(node));
        // reset the state of the repository
        incorrectFile.delete();
        graph.update(node);
    }

    /**
     * Test that the function validFolderName() works correctly for valid folder names. It should return true
     * for all folders in the repository.
     */
    @Test
    public void testValidFolderName() {
        Deque<FileGraph.Node> stack = new ArrayDeque<>();
        FileGraph.Node root = graph.getRoot();
        graph.update(root);
        stack.push(root);
        // recursively iterate through the file graph
        while(!stack.isEmpty()) {
            FileGraph.Node node = stack.pop();
            if(node == errorNode) continue;
            String folderName = FileTools.getNameWithoutPrefix(root.path, node.path);
            // should return true on every folder by definition
            assertTrue(checker.validFolderName(folderName));

            for(FileGraph.Node children : node.children.values()) {
                stack.push(children);
            }
        }
    }



    /**
     * Test the validFolderStructure() function. It should return true on every path that was found in the unchanged
     * file graph.
     */
    @Test
    public void testValidFolderStructure() {
        testValidFolderStructureRec(graph.getRoot(), new ArrayList<>());
    }

    /**
     * Recursive implementation of the testValidFolderStructure() test function.
     * @param node current node
     * @param folders current path to the node as a list of string folders
     */
    private void testValidFolderStructureRec(FileGraph.Node node, List<String> folders) {
        if(node == errorNode) return;
        String folderName = FileTools.getNameWithoutPrefix(graph.getRoot().path, node.path);
        folders.add(folderName);
        // only call the function on leaf nodes when the path is complete
        if(!node.leaf) {
            for(FileGraph.Node next : node.children.values()) {
                testValidFolderStructureRec(next, folders);
            }
        } else {
            // should always return true
            assertTrue(checker.validFolderStructure(folders));
        }

        folders.remove(folders.size()-1);
    }

    /**
     * This tests that the validFolderStructure() function should return false on an incorrect structure.
     */
    @Test
    public void testValidFolderStructureFalse() {
        // "2010_asdf" is in invalid structure because "asdf" is not a correct name for a month
        List<String> folders = List.of("", "2010", "2010_asdf");
        assertFalse(checker.validFolderStructure(folders));
    }

    /**
     * Call the checkAll() function and make sure that no errors were found as the file graph is correct.
     */
    @Test
    public void checkAllCorrect() {
        checker.checkAll();
        String out = "";
        // add the error to the out string if the list of nodes is not empty
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : checker.getErrors().entrySet()) {
            if(!e.getValue().isEmpty()) out += e + "\n";
        }
        // out should be empty because there are no errors
        if(!out.isEmpty()) fail("errors detected even though everything is correct: \n" + out);
    }

    /**
     * Call the checkAll() function after injecting multiple files in the wrong locations. Each error should appear
     * exactly once.
     */
    @Test
    public void checkAllTestIncorrectSingle() {
        // create a file in the wrong folder
        File fileWrongFolder = new File(repoPath+File.separator+"1970", "wrong_folder.txt");

        try {
            new File(repoPath+File.separator+"1970").mkdirs();
            fileWrongFolder.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // create a folder with an incorrect folder name
        File folderInvalidName = new File(repoPath+File.separator+"el_wiwi");
        try {
            folderInvalidName.mkdirs();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // create a file in a folder so that the threshold is exceeded
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

        // add a file in a non leaf node
        File folderNonLeaf = new File(repoPath+File.separator+"2023");
        File fileNonLeaf = new File(folderNonLeaf, "file_inner_node.txt");
        try {
            fileNonLeaf.createNewFile();
            fileNonLeaf.setLastModified(1675387890214l);
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // delete the mandatory error folder that should always exist in a repository
        File errorFolder = new File(repoPath + File.separator + Configuration.ERROR_FOLDER_NAME);
        errorFolder.delete();

        // update the graph after the changes
        graph.update(graph.getRoot());
        // execute the checker function that checks for all errors at once
        checker.checkAll();

        // check if all errors have been found
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();

        for(ModelError me : ModelError.values()) {
            assertEquals(1, errors.get(me).size());
        }

        assertEquals(errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES).get(0).path, fileWrongFolder.getParentFile().getAbsolutePath());
        assertEquals(errors.get(ModelError.INVALID_FOLDER_NAME).get(0).path, folderInvalidName.getAbsolutePath());
        assertEquals(errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).get(0).path, folderAboveThreshold.getAbsolutePath());
        assertEquals(errors.get(ModelError.FILES_IN_NON_LEAF).get(0).path, folderNonLeaf.getAbsolutePath());
        assertEquals(errors.get(ModelError.INVALID_FOLDER_STRUCTURE).get(0).path, folderInvalidName.getAbsolutePath());

        // delete the temporarily created files
        FileTools.delete(fileWrongFolder.getParentFile());
        FileTools.delete(folderInvalidName);
        FileTools.delete(folderAboveThreshold);
        FileTools.delete(fileNonLeaf);
        errorFolder.mkdir();

        // update the static variables to their initial state
        graph.update(graph.getRoot());
        FileGraph.Node root = graph.getRoot();
        errorNode = root.children.get(root.path + File.separator + Configuration.ERROR_FOLDER_NAME);
    }

    /**
     * Create a folder containing multiple errors and run the checkAll() function. All errors should be found. The folder
     * will have three errors: invalid name, above threshold, and wrong folder structure.
     */
    @Test
    public void testCheckAllIncorrectMultiple() {
        // create a folder with an incorrect name. all folder names should be parsed dates separated by underscores
        File folderInvalidNameAboveThreshold = new File(repoPath+File.separator+"el_wiwi");
        // create two files to insert into the folder exceeding the threshold of one
        File f0 = new File(folderInvalidNameAboveThreshold, "el_wiwi0.png");
        File f1 = new File(folderInvalidNameAboveThreshold, "el_wiwi1.png");

        try {
            folderInvalidNameAboveThreshold.mkdirs();
            f0.createNewFile();
            f1.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // update the graph after the changes to the file graph and execute the function
        graph.update(graph.getRoot());
        checker.checkAll();

        // count the number of errors
        int numErr = 0;
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : checker.getErrors().entrySet()) {
            numErr += e.getValue().size();
        }
        // the count should be equal to three: incorrect name, above threshold, and invalid folder structure
        assertEquals(3, numErr);

        // reset the repo by deleting the created files
        FileTools.delete(folderInvalidNameAboveThreshold);
    }
}
