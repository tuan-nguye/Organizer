package tests.classes;

import com.org.util.consistency.Checker;
import com.org.util.time.DateExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.Copy;
import com.org.organizer.copy.ICopy;
import com.org.organizer.copy.Move;
import com.org.parser.Configuration;
import tests.resources.GenerateExampleFiles;
import tests.resources.InitializeTestRepository;
import com.org.util.FileTools;
import com.org.util.consistency.ModelChecker;
import com.org.util.consistency.ModelError;
import com.org.util.consistency.ModelFixer;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        graph.update(graph.getRoot());
        fixer = new ModelFixer(config);
        checker = new ModelChecker(config);
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
        checker.checkAll();
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();

        // fix structure and check if everything is correct
        fixer.fixStructure(errors); // reorganizing messes up files
        checker.checkAll();

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
        File folder2010Feb1615h_9min_10s = new File(repoPath+File.separator+"2010"+File.separator+"2010_feb"+File.separator+"2010_feb_16"+File.separator+"2010_feb_16_15h"+File.separator+"2010_feb_16_15h_9min"+File.separator+"2010_feb_16_15h_9min_10s");
        File reduceTxt = new File(folder2010Feb1615h_9min_10s, "reduce.txt");
        long lm_reduce = FileTools.epochMilli(LocalDateTime.of(2010, 2, 16, 0, 0));

        try {
            File folder2010Jul = new File(repoPath+File.separator+"2010"+File.separator+"2010_jul");
            folder2010Jul.mkdir();
            move.execute(test2Txt.toPath(), folder2010Jul.toPath().resolve(test2Txt.getName()));
            folder2010Feb1615h_9min_10s.mkdirs();
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

        checker.checkAll();
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();
        assertEquals(3, errors.get(ModelError.INVALID_FOLDER_STRUCTURE).size());
        fixer.fixStructure(errors);

        checker.checkAll();
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
        Map<ModelError, List<FileGraph.Node>> errors = new HashMap<>();
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 1, 0, 0);
        errors.get(ModelError.INVALID_FOLDER_NAME).add(graph.getNode(ldt));
        errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(graph.getNode(ldt));

        ldt = LocalDateTime.of(2010, 1, 1, 0, 0);
        errors.get(ModelError.CAN_BE_REDUCED).add(graph.getNode(ldt));
        errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES).add(graph.getNode(ldt));

        errors.get(ModelError.FILES_IN_NON_LEAF).add(graph.getRoot().children.get(graph.getRoot().path+File.separator+"2023"));

        ldt = LocalDateTime.of(2023, 2, 1, 0, 0);
        errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).add(graph.getNode(ldt));

        fixer.fixStructure(errors);
        checker.checkAll();

        for(List<FileGraph.Node> list : checker.getErrors().values()) {
            assertEquals(0, list.size());
        }

        resetRepo();
    }

    @Test
    public void moveToErrorFolder() {
        File corruptJpgNonLeaf = new File(repoPath + File.separator + "2023", "image.jpg");
        File corruptJpgLeaf = new File(repoPath + File.separator + "2010", "image2.jpg");

        try {
            corruptJpgNonLeaf.createNewFile();
            corruptJpgLeaf.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        graph.update(graph.getRoot());
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        File jpgCorrectLocation = new File(repoPath + File.separator + "error", "image.jpg");
        File jpg2CorrectLocation = new File(repoPath + File.separator + "error", "image2.jpg");
        assertTrue(jpgCorrectLocation.exists());
        assertTrue(jpg2CorrectLocation.exists());

        resetRepo();
    }

    @Test
    public void errorFolderMissing() {
        File errorFolder = new File(repoPath + File.separator + Configuration.ERROR_FOLDER_NAME);
        errorFolder.delete();

        graph.update(graph.getRoot());
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        assertTrue(errorFolder.exists());

        resetRepo();
    }

    @Test
    public void duplicateRenameTest() {
        File duplicate2Src = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new Copy(), move = new Move();
        File duplicate2 = new File(repoPath + File.separator + "2023" + File.separator + "2023_feb", "test2.txt");
        File test4 = new File(repoPath + File.separator + "2021", "test4.txt");
        File test4NewLocation = new File(repoPath + File.separator + "2021" + File.separator + "2021_aug", "test4.txt");
        File test4_0 = new File(repoPath + File.separator + "2021" + File.separator + "2021_jul", "test4.txt");

        try {
            copy.execute(duplicate2Src.toPath(), duplicate2.toPath());
            long day = 1000*60*60*24;
            duplicate2.setLastModified(duplicate2.lastModified()-day);
            test4_0.getParentFile().mkdirs();
            copy.execute(test4.toPath(), test4_0.toPath());
            long month = 1000L*60*60*24*31;
            test4_0.setLastModified(test4_0.lastModified()-month);
            test4NewLocation.getParentFile().mkdirs();
            move.execute(test4.toPath(), test4NewLocation.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        graph.update(graph.getRoot());
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        File duplicate2InCorrectFolder = new File(repoPath + File.separator + "2010", "test2(1).txt");
        assertTrue(duplicate2InCorrectFolder.exists());
        File duplicate4InCorrectFolder = new File(repoPath + File.separator + "2021", "test4.txt");
        assertTrue(duplicate4InCorrectFolder.exists());
        File duplicate4_0InCorrectFolder = new File(repoPath + File.separator + "2021", "test4(1).txt");
        assertTrue(duplicate4_0InCorrectFolder.exists());

        resetRepo();
    }

    @Test
    public void duplicateIgnore() {
        File test2 = new File(repoPath + File.separator + "2010" + File.separator + "test2.txt");
        File test2_0 = new File(repoPath + File.separator + "2010" + File.separator + "2010_jul", "test2.txt");
        File test4 = new File(repoPath + File.separator + "2021", "test4.txt");
        File test4_0 = new File(repoPath + File.separator + "2023" + File.separator + "2023_feb", "test4.txt");
        ICopy copy = new Copy();
        int numFilesBefore = FileTools.countFiles(new File(repoPath));

        try {
            test2_0.getParentFile().mkdirs();
            copy.execute(test2.toPath(), test2_0.toPath());
            copy.execute(test4.toPath(), test4_0.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        graph.update(graph.getRoot());
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        int numFilesAfter = FileTools.countFiles(new File(repoPath));
        assertEquals(numFilesBefore, numFilesAfter);
        resetRepo();
    }
}
