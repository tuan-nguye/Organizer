package tests.classes;

import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.Copy;
import com.org.parser.Configuration;
import com.org.util.FileTools;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelFixer;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;
import com.org.util.time.DateExtractor;
import com.org.util.time.MarkAllFiles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tests.resources.GenerateExampleFiles;
import tests.resources.InitializeTestRepository;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * This class tests the functionality of the MarkFiles class for marking files to increase
 * their performance for later access.
 */
public class MarkTest {
    // string path to the repository to test on
    private static final String repoPath = Path.of("test-bin/repoMark").toAbsolutePath().toString();
    // configuration object to execute commands
    private static Configuration config;
    // file graph reference
    private static FileGraph graph;
    // folder size threshold
    private static int threshold = 2;

    /** These test files will be used. The given paths are how they should look like after organization.
     * "test-bin/repo/2010/test2.txt",
     * "test-bin/repo/2021/test4.txt",
     * "test-bin/repo/2023/2023_feb/test1.txt",
     * "test-bin/repo/2023/2023_märz/test3.txt",
     * "test-bin/repo/2023/2023_märz/test0.txt"
     */

    /**
     * Preparation function. It creates a new configuration object and gets the filegraph
     * object.
     */
    @BeforeAll
    public static void prepare() {
        config = new Configuration();
        graph = FileGraphFactory.get(repoPath);
        resetRepo();
    }

    /**
     * reset repo to initial files and directory state. Execute this function if
     * any changes were made to the repository after testing.
     */
    private static void resetRepo() {
        // delete all files in the repo and copy them into it again
        FileTools.delete(new File(repoPath));
        graph.update(graph.getRoot());
        InitializeTestRepository.generateRepository(repoPath, config, threshold);
        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold, repoPath);
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath);
        // update the file graph state
        graph.update(graph.getRoot());
    }

    /**
     * Check that every file in the repository is marked.
     */
    @Test
    public void markAllTest() {
        DateExtractor.setIgnoreMark(true);
        File repo = new File(repoPath);
        MarkAllFiles marker = new MarkAllFiles(repoPath);
        marker.execute();

        assertTrue(markedDfs(repo));
    }

    /**
     * Recursive implementation for checking the all the given file and all of its
     * subfolders and subfiles are marked correctly.
     * @param file current file
     * @return true if all files in the subtree are marked, false if at least one is not
     */
    private boolean markedDfs(File file) {
        if(file.isFile()) {
            if(!DateExtractor.fileIsMarked(file)) return false;
        } else {
            for(File f : file.listFiles()) {
                if(!markedDfs(f)) return false;
            }
        }

        return true;
    }
}
