package tests.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import organizer.copy.ICopy;
import organizer.copy.Move;
import parser.Configuration;
import resources.GenerateExampleFiles;
import resources.InitializeTestRepository;
import util.FileTools;
import util.consistency.ModelChecker;
import util.consistency.ModelError;
import util.consistency.ModelFixer;
import util.graph.FileGraph;
import util.graph.FileGraphFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        graph = FileGraphFactory.get(repoPath);
        resetRepo();

        checker = new ModelChecker(config);
        fixer = new ModelFixer(config);
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
    }

    /**
     * inserted errors:
     *   - number of files above threshold
     *   - file in the wrong folder
     *   - file in inner folder instead of leaf
     *   - empty folder with invalid name
     */
    @Test
    public void fixMultipleErrorsByMovingTest() {
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

        graph.update(graph.getRoot());
        checker.checkAll(true, true);
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();

        // fix structure and check if everything is correct
        fixer.fixStructure(errors, true, true); // reorganizing messes up files
        checker.checkAll(true, true);

        errors = checker.getErrors();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            assertEquals(0, e.getValue().size());
        }

        // clean up
        resetRepo();
    }

    /**
     * add useless folders and empty ones
     * structure should be reduced to be minimal
     */
    @Test
    public void reduceTest() {
        ICopy move = new Move();
        File test2Txt = new File(repoPath+File.separator+"2010", "test2.txt");
        File folder2010Feb16 = new File(repoPath+File.separator+"2010"+File.separator+"2010_feb"+File.separator+"2010_feb_16");
        File reduceTxt = new File(folder2010Feb16, "reduce.txt");
        long lm_reduce = FileTools.epochMilli(LocalDateTime.of(2010, 2, 16, 0, 0));

        try {
            File folder2010Jul = new File(repoPath+File.separator+"2010"+File.separator+"2010_jul");
            folder2010Jul.mkdir();
            move.execute(test2Txt.toPath(), folder2010Jul.toPath().resolve(test2Txt.getName()));
            folder2010Feb16.mkdirs();
            reduceTxt.createNewFile();
            reduceTxt.setLastModified(lm_reduce);
            File folder2020Oct = new File(repoPath+File.separator+"2010"+File.separator+"2010_oct");
            folder2020Oct.mkdir();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        graph.update(graph.getRoot());
        fixer.reduceStructure();
        assertTrue(new File(repoPath+File.separator+"2010", "test2.txt").exists());
        assertTrue(new File(repoPath+File.separator+"2010", "reduce.txt").exists());
        assertEquals(2, new File(repoPath+File.separator+"2010").listFiles(a->a.isFile()).length);
        assertEquals(0, new File(repoPath+File.separator+"2010").listFiles(a->a.isDirectory()).length);

        // clean up
        resetRepo();
    }

    /**
     * test whether folder names are restored correctly
     */
    @Test
    public void restoreFolderNamesTest() {
        File folder2023März = new File(repoPath+File.separator+"2023"+File.separator+"2023_märz");
        File folder2023 = new File(repoPath+File.separator+"2023");
        File folderRandom = new File(repoPath+File.separator+"2023"+File.separator+"random");
        File fileRandom = new File(folderRandom, "random.txt");

        try {
            folderRandom.mkdir();
            fileRandom.createNewFile();
            fileRandom.setLastModified(FileTools.epochMilli(LocalDateTime.of(2008, 1, 1, 0, 0)));
            folder2023März.renameTo(new File(folder2023März.getParentFile(), "el_wiwi"));
            folder2023.renameTo(new File(folder2023.getParentFile(), "shttng_tthpst"));
        } catch(Exception e) {
            fail(e.getMessage());
        }
        graph.update(graph.getRoot());

        checker.checkAll(true, true);
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();
        assertEquals(3, errors.get(ModelError.INVALID_FOLDER_STRUCTURE).size());
        fixer.fixStructure(errors, true, true);

        checker.checkAll(true, true);
        errors = checker.getErrors();
        for(List<FileGraph.Node> errorList : errors.values()) {
            assertEquals(0, errorList.size());
        }

        resetRepo();
    }

    /**
     * idempotent, fixing a valid structure should not change anything
     * simulate by executing on fake errors
     */
    @Test
    public void correctStructureTest() {

    }

    @Test
    public void test() {
        File folder2023 = new File(repoPath + File.separator + "2023");
        folder2023.renameTo(new File(repoPath + File.separator + "2022"));
        graph.update(graph.getRoot());
        checker.checkAll(true, true);
        fixer.fixStructure(checker.getErrors(), true, true);
    }
}
