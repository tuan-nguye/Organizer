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

public class MarkTest {
    private static final String repoPath = Path.of("test-bin/repoMark").toAbsolutePath().toString();
    private static Configuration config;
    private static FileGraph graph;
    private static int threshold = 2;

    /**
     * "test-bin/repo/2010/test2.txt",
     * "test-bin/repo/2021/test4.txt",
     * "test-bin/repo/2023/2023_feb/test1.txt",
     * "test-bin/repo/2023/2023_märz/test3.txt",
     * "test-bin/repo/2023/2023_märz/test0.txt"
     */

    @BeforeAll
    public static void prepare() {
        config = new Configuration();
        graph = FileGraphFactory.get(repoPath);
        resetRepo();
    }

    /**
     * reset repo to initial files and directory state
     */
    private static void resetRepo() {
        FileTools.delete(new File(repoPath));
        graph.update(graph.getRoot());
        InitializeTestRepository.generateRepository(repoPath, config, threshold);
        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold, repoPath);
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath);
        graph.update(graph.getRoot());
    }

    @Test
    public void markAllTest() {
        DateExtractor.setIgnoreMark(true);
        File repo = new File(repoPath);
        MarkAllFiles marker = new MarkAllFiles(repoPath);
        marker.execute();

        assertTrue(markedDfs(repo));
    }

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
