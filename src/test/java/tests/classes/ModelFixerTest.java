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

/**
 * Class that contains all tests for the ModelFixer class.
 */
public class ModelFixerTest {
    // string path to the repository to perform the tests in
    private static final String repoPath = Path.of("test-bin/repoModelFix").toAbsolutePath().toString();
    // configuration object for executing the commands
    private static Configuration config;
    // file graph object
    private static FileGraph graph;
    // model checker object for finding the errors
    private static ModelChecker checker;
    // model fixer object
    private static ModelFixer fixer;
    // folder siize threshold
    private static int threshold = 2;

    /** Prepares all objects and sets them to their initial state.
     *
     * correct paths for each example txt file:
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
        // generate the test files and copy them over to the repo
        InitializeTestRepository.generateRepository(repoPath, config, threshold);
        Organizer organizer = new ThresholdOrganizer(new Copy(), threshold, repoPath);
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath);
        // update the graph
        graph.update(graph.getRoot());
        fixer = new ModelFixer(config);
        checker = new ModelChecker(config);
    }

    /**
     * Test that the fixStructure() function repairs all error found by the checker and none remain after fixing.
     * inserted errors:
     *   - number of files above threshold
     *   - file in the wrong folder
     *   - file in inner folder instead of leaf
     *   - empty folder with invalid name
     */
    @Test
    public void fixMultipleErrorsByMovingTest() {
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

        // insert too many files in a folder, number of files above threshold
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

        // add files to  non leaf node
        File folderNonLeaf = new File(repoPath+File.separator+"2023");
        File fileNonLeaf = new File(folderNonLeaf, "file_inner_node.txt");
        try {
            fileNonLeaf.createNewFile();
            fileNonLeaf.setLastModified(1675387890214l);
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // update the file graph and run the checker to get all the errors
        graph.update(graph.getRoot());
        checker.checkAll();
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();

        // fix structure and check if everything is correct
        fixer.fixStructure(errors); // reorganizing messes up files
        checker.checkAll();

        // no errors should be there after fixing
        errors = checker.getErrors();
        for(Map.Entry<ModelError, List<FileGraph.Node>> e : errors.entrySet()) {
            assertEquals(0, e.getValue().size());
        }

        // clean up and reset state
        resetRepo();
    }

    /**
     * Useless folders and empty ones are added to the file graph. The reduceStructure() function should detect
     * them and reduce the file graph and delete empty folders to minimize the graph.
     */
    @Test
    public void reduceTest() {
        ICopy move = new Move();
        File test2Txt = new File(repoPath+File.separator+"2010", "test2.txt");
        // create a folder that is way too deep for the little amount of files
        File folder2010Feb1615h_9min_10s = new File(repoPath+File.separator+"2010"+File.separator+"2010_feb"+File.separator+"2010_feb_16"+File.separator+"2010_feb_16_15h"+File.separator+"2010_feb_16_15h_9min"+File.separator+"2010_feb_16_15h_9min_10s");
        File reduceTxt = new File(folder2010Feb1615h_9min_10s, "reduce.txt");
        long lm_reduce = FileTools.epochMilli(LocalDateTime.of(2010, 2, 16, 0, 0));

        try {
            // move the text file to a folder deeper without changing the amount of files
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
        // update the graph and execute the reduction function
        graph.update(graph.getRoot());
        fixer.reduceStructure();
        // check that the test files are in the correct folder afterwards
        assertTrue(new File(repoPath+File.separator+"2010", "test2.txt").exists());
        assertTrue(new File(repoPath+File.separator+"2010", "reduce.txt").exists());
        assertEquals(2, new File(repoPath+File.separator+"2010").listFiles(a->a.isFile()).length);
        assertEquals(0, new File(repoPath+File.separator+"2010").listFiles(a->a.isDirectory()).length);

        // clean up
        resetRepo();
    }

    /**
     * Test whether folder names are restored correctly by executing the function fixFolders(). Files should not be
     * moved. The repairing should only be done by renaming folders to their correct name.
     */
    @Test
    public void restoreFolderNamesTest() {
        File folder2023 = new File(repoPath+File.separator+"2023");
        File folder2023März = new File(folder2023, "2023_märz");
        File folderRandom = new File(repoPath+File.separator+"2023"+File.separator+"random");
        File fileRandom = new File(folderRandom, "random.txt");

        try {
            // create another folder and a file that belongs in the 2008 year folder structure to the 2023 folder
            // this folder should not be possible to fix as it would break the structure
            folderRandom.mkdir();
            fileRandom.createNewFile();
            fileRandom.setLastModified(FileTools.epochMilli(LocalDateTime.of(2008, 1, 1, 0, 0)));
            // rename the folders to incorrect names
            folder2023März.renameTo(new File(folder2023, "el_wiwi"));
            folder2023.renameTo(new File(folder2023.getParentFile(), "shttng_tthpst"));
        } catch(Exception e) {
            fail(e.getMessage());
        }
        // update the file graph after changes
        graph.update(graph.getRoot());

        // get errors after checking
        checker.checkAll();
        Map<ModelError, List<FileGraph.Node>> errors = checker.getErrors();
        // three errors are expected: /shttng_tthpst/el_wiwi, /shttng_tthpst/2023_feb, /shttng_tthpst/random
        assertEquals(3, errors.get(ModelError.INVALID_FOLDER_STRUCTURE).size());
        // fixer should be able to fix two errors by renaming folders
        fixer.fixFolders(errors);

        // check for errors again
        checker.checkAll();
        // now after fixing the one or more of the folder's names only some folders should still have an error because it's
        // impossible to fix by renaming. The file should be moved to the right location, which is not tested in this function
        errors = checker.getErrors();
        int errorCount = errors.get(ModelError.INVALID_FOLDER_STRUCTURE).size();
        System.out.println(errors.get(ModelError.INVALID_FOLDER_STRUCTURE));
        assertTrue(errorCount < 3);

        // reset the repo state
        resetRepo();
    }

    /**
     * idempotent fixStructure() function, fixing a valid structure should not change anything
     * simulate by executing on fake errors
     */
    @Test
    public void correctStructureTest() {
        // make up some errors
        Map<ModelError, List<FileGraph.Node>> errors = new HashMap<>();
        for(ModelError me : ModelError.values()) errors.put(me, new ArrayList<>());
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 1, 0, 0);
        // add the errors to the map
        errors.get(ModelError.INVALID_FOLDER_NAME).add(graph.getNode(ldt));
        errors.get(ModelError.INVALID_FOLDER_STRUCTURE).add(graph.getNode(ldt));
        ldt = LocalDateTime.of(2010, 1, 1, 0, 0);
        errors.get(ModelError.CAN_BE_REDUCED).add(graph.getNode(ldt));
        errors.get(ModelError.FOLDER_CONTAINS_INCONSISTENT_DATES).add(graph.getNode(ldt));
        errors.get(ModelError.FILES_IN_NON_LEAF).add(graph.getRoot().children.get(graph.getRoot().path+File.separator+"2023"));
        ldt = LocalDateTime.of(2023, 2, 1, 0, 0);
        errors.get(ModelError.FOLDER_ABOVE_THRESHOLD).add(graph.getNode(ldt));

        // execute the function and check that no errors were created by the function
        fixer.fixStructure(errors);
        checker.checkAll();

        // check that there are no errors
        for(List<FileGraph.Node> list : checker.getErrors().values()) {
            assertEquals(0, list.size());
        }

        // reset repo state
        resetRepo();
    }

    /**
     * Create two corrupt jpg files and insert them into the file graph. After repairing them, they should
     * be located in the error folder.
     */
    @Test
    public void moveToErrorFolder() {
        // creating jpg files with createNewFile() will lead to corrupt jpgs
        File corruptJpgNonLeaf = new File(repoPath + File.separator + "2023", "image.jpg");
        File corruptJpgLeaf = new File(repoPath + File.separator + "2010", "image2.jpg");

        try {
            corruptJpgNonLeaf.createNewFile();
            corruptJpgLeaf.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        // update the file graph after creating the files
        graph.update(graph.getRoot());
        // search for the errors and fix the structure
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        // both jpgs should now be in the error folder
        File jpgCorrectLocation = new File(repoPath + File.separator + "error", "image.jpg");
        File jpg2CorrectLocation = new File(repoPath + File.separator + "error", "image2.jpg");
        assertTrue(jpgCorrectLocation.exists());
        assertTrue(jpg2CorrectLocation.exists());

        // reset the repo state
        resetRepo();
    }

    /**
     * The error folder will be deleted. This test will check whether the error folder is restored when it's missing.
     */
    @Test
    public void errorFolderMissing() {
        File errorFolder = new File(repoPath + File.separator + Configuration.ERROR_FOLDER_NAME);
        errorFolder.delete();
        // update the file graph, search for errors, and fix the structure
        graph.update(graph.getRoot());
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());
        // the error folder should exist
        assertTrue(errorFolder.exists());
        // reset the repo state
        resetRepo();
    }

    /**
     * Duplicates can exist in the file graph. This happens if two files have the same name but have different datetime
     * stamps. The files will be considered as different and renamed before being moved. This test makes sure that the
     * renaming happens not only when organizing with the Organizer class, but also when executing the ModelFixer.
     */
    @Test
    public void duplicateRenameTest() {
        // create duplicates of the test2.txt file, see example files created by InitializeTestRepository
        // insert them in random locations
        File duplicate2Src = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new Copy(), move = new Move();
        File duplicate2 = new File(repoPath + File.separator + "2023" + File.separator + "2023_feb", "test2.txt");
        // also create duplicates of the test4.txt file, again see example files
        // insert them in random locations
        File test4 = new File(repoPath + File.separator + "2021", "test4.txt");
        File test4NewLocation = new File(repoPath + File.separator + "2021" + File.separator + "2021_aug", "test4.txt");
        File test4_0 = new File(repoPath + File.separator + "2021" + File.separator + "2021_jul", "test4.txt");

        // change the lastModified field by a little to make sure they are seen as different files
        // and then copy them
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

        // update the file graph
        graph.update(graph.getRoot());
        // search for errors and fix them
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        // check that the files were renamed correctly and are located in the correct directory
        File duplicate2InCorrectFolder = new File(repoPath + File.separator + "2010", "test2(1).txt");
        assertTrue(duplicate2InCorrectFolder.exists());
        File duplicate4InCorrectFolder = new File(repoPath + File.separator + "2021", "test4.txt");
        assertTrue(duplicate4InCorrectFolder.exists());
        File duplicate4_0InCorrectFolder = new File(repoPath + File.separator + "2021", "test4(1).txt");
        assertTrue(duplicate4_0InCorrectFolder.exists());

        // reset repo state
        resetRepo();
    }

    /**
     * Make copies of some files and copy them as is into the file graph. The fixer should be able to tell, that the
     * files are the same and not rename them. Instead, they are going to be replaced or ignored.
     */
    @Test
    public void duplicateIgnore() {
        // create duplicates of text2.txt and test4.txt
        File test2 = new File(repoPath + File.separator + "2010" + File.separator + "test2.txt");
        File test2_0 = new File(repoPath + File.separator + "2010" + File.separator + "2010_jul", "test2.txt");
        File test4 = new File(repoPath + File.separator + "2021", "test4.txt");
        File test4_0 = new File(repoPath + File.separator + "2023" + File.separator + "2023_feb", "test4.txt");
        ICopy copy = new Copy();
        int numFilesBefore = FileTools.countFiles(new File(repoPath));

        // copy the duplicate files into the file graph but this time without changing the file's lastModified field
        try {
            test2_0.getParentFile().mkdirs();
            copy.execute(test2.toPath(), test2_0.toPath());
            copy.execute(test4.toPath(), test4_0.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        // update the graph
        graph.update(graph.getRoot());
        // search for errors and execute the fixer
        checker.checkAll();
        fixer.fixStructure(checker.getErrors());

        // the number of files shouldn't have changed because duplicates are ignored
        int numFilesAfter = FileTools.countFiles(new File(repoPath));
        assertEquals(numFilesBefore, numFilesAfter);

        // reset the repo state even if it shouldn't have changed just to be sure
        resetRepo();
    }
}
