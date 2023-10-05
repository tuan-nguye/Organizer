package tests.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import parser.Configuration;
import resources.GenerateExampleFiles;
import resources.InitializeTestRepository;
import util.FileTools;
import util.consistency.ModelChecker;
import util.consistency.ModelElement;
import util.consistency.ModelError;
import util.consistency.ModelFixer;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ModelFixerTest {
    private static final String repoPath = Path.of("test-bin/repoModelFix").toAbsolutePath().toString();
    private static Configuration config;
    private static FileGraph graph;
    private static ModelChecker checker;
    private static ModelFixer fixer;
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
        InitializeTestRepository.generateRepository(repoPath, config, threshold);

        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold);
        organizer.allowExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath, repoPath);

        graph = new FileGraph(repoPath);
        checker = new ModelChecker(config);
        fixer = new ModelFixer(config);
    }

    /**
     * insertedd errors:
     *   - number of files above threshold
     *   - file in the wrong folder
     *   - file in inner folder instead of leaf
     *   - empty folder with invalid name
     */
    @Test
    public void fixMultipleErrors() {
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
            File f2 = new File(folderAboveThreshold, "above_thresh2.txt");
            f2.createNewFile();
            f2.setLastModified(lm+24*60*60*1000); // one day later
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

        checker.checkAll(true, true);
        Map<ModelError, List<ModelElement>> errors = checker.getErrors();

        // fix structure and check if everything is correct
        fixer.fixStructure(errors, true, true);
        checker.checkAll(true, true);

        errors = checker.getErrors();
        for(Map.Entry<ModelError, List<ModelElement>> e : errors.entrySet()) {
            assertEquals(0, e.getValue().size());
        }

        // clean up
        FileTools.delete(new File(repoPath));
        InitializeTestRepository.generateRepository(repoPath, config, threshold);
        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold);
        organizer.allowExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath, repoPath);
        graph.update(graph.getRoot());
    }

    @Test
    public void test() {
        String path = "/a/ab/bb/c";
        String[] split = path.split(File.separator);
        System.out.println(Arrays.toString(split));
    }
}
