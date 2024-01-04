package tests.classes;

import com.org.organizer.Organizer;
import com.org.organizer.copy.*;
import com.org.util.graph.FileGraph;
import com.org.util.graph.FileGraphFactory;
import com.org.util.time.DateExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.org.organizer.ThresholdOrganizer;
import com.org.parser.Configuration;
import tests.resources.GenerateExampleFiles;
import tests.resources.InitializeTestRepository;
import com.org.util.FileTools;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class containing tests for the ThresholdOrganizer.
 */
public class ThresholdOrganizerTest {
    // absolute path as string to the test repository
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();
    // organizer object
    private static ThresholdOrganizer organizer;
    // file graph object
    private static FileGraph fileGraph;

    /**
     * Initialize the objects and set up the test files. But the repository is still empty
     */
    @BeforeAll
    public static void prepare() {
        Configuration conf = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, conf, 1);

        organizer = new ThresholdOrganizer(new CopyReplace(), 1, repoPath);
        organizer.fileExtensionAllowed("txt");
        fileGraph = FileGraphFactory.get(repoPath);
    }

    /**
     * Copy the test files into the repository with the organizer. Then, check that the files are all
     * in their correct directories.
     */
    @Test
    public void testCopyAndOrganize() {
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"txt");

        // string array containing the correct locations for each txt file
        String[] files = new String[] {
                "test-bin/repo/2010/test2.txt",
                "test-bin/repo/2021/test4.txt",
                "test-bin/repo/2023/2023_feb/test1.txt",
                "test-bin/repo/2023/2023_märz/2023_märz_17/test3.txt",
                "test-bin/repo/2023/2023_märz/2023_märz_21/test0.txt"
        };

        // all files should exist in that directory if they've been organized correctly
        for(String fileStr : files) {
            File file = new File(fileStr);
            assertTrue(file.exists());
        }

        // make sure that the number of file is correct, length+1 because the hidden configuration file is included
        assertEquals(files.length+1, FileTools.countFiles(new File(repoPath)));
    }

    /**
     * Test the allowed file extensions for the organizer. Set only txt files as allowed extension and copy the files
     * to the repository. The csv file must not be in the repository.
     */
    @Test
    public void addUnallowedFileExtension() {
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"csv");
        // csv files are not allowed, so none should appear in the repo
        assertEquals(0, FileTools.countFiles(new File(repoPath), (dir, name) -> name.contains("csv")));
    }

    /**
     * Organizing a corrupted jpg should copy them to the error folder. Test that the file is in there after executing
     * the organizer.
     */
    @Test
    public void copyCorruptFileTest() {
        File corruptJpg = new File("test-bin", "image.jpg");

        if(!corruptJpg.exists()) {
            try {
                corruptJpg.createNewFile();
            } catch(Exception e) {
                fail(e.getMessage());
            }
        }

        // organize the corrupted jpg into the repository
        organizer.allowFileExtension("jpg");
        organizer.copyAndOrganize(corruptJpg.getAbsolutePath());

        // the corrupted file should be in the error folder
        File jpgCorrectFolder = new File(repoPath + File.separator + Configuration.ERROR_FOLDER_NAME, "image.jpg");
        assertTrue(jpgCorrectFolder.exists());
        // restore the repo's state
        jpgCorrectFolder.delete();
        fileGraph.update(fileGraph.getRoot());
    }

    /**
     * Duplicates should be replaced in the file graph. This test makes sure that the duplicate doesn't appear twice.
     * And the test also checks that the original file doesn't exist after moving the file instead of copying.
     */
    @Test
    public void duplicateReplaceTest() {
        File duplicateSrc = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new CopyReplace();
        File duplicate = new File("test-bin", "test2.txt");
        File folder2010 = new File(repoPath, "2010");
        // create new organizer objects that allow more than a single file
        Organizer orgCopy = new ThresholdOrganizer(copy, 3, repoPath);
        Organizer orgMove = new ThresholdOrganizer(new MoveReplace(), 2, repoPath);

        try {
            copy.execute(duplicateSrc.toPath(), duplicate.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        // copy the file into the file graph, it should exist once in the folder /root/2010 now
        orgCopy.copyAndOrganize(duplicate.getAbsolutePath());
        // now move the same file into the graph again, duplicate should be replaced, it should still exist only once
        orgMove.copyAndOrganize(duplicate.getAbsolutePath());

        // test that only one file is in the destination directory
        assertEquals(1, FileTools.countFiles(folder2010));
        // test that the source file is gone, because it was moved and replaced in the last operation
        assertFalse(duplicate.exists());

        // cleanup and delete the file and its folder
        FileTools.delete(folder2010);
        fileGraph.update(fileGraph.getRoot());
    }

    /**
     * Files with the same name but differing datetime stamps are seen as different files. So upon copying into the
     * repo they should be renamed. This test checks whether the files are renamed correctly. None of the files are
     * supposed to be replaced. They should all exist but under different names. For this a test txt file will be
     * copied three times into the directory and the date will be changed slightly inbetween.
     */
    @Test
    public void duplicateRenameTest() {
        File duplicateSrc = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new Copy();
        File duplicate = new File("test-bin", "test2.txt");
        Organizer org = new ThresholdOrganizer(copy, 3, repoPath);

        // copy the temporare file into the test-bin directory
        try {
            copy.execute(duplicateSrc.toPath(), duplicate.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        // copy the original txt file
        org.copyAndOrganize(duplicateSrc.getAbsolutePath());
        // go back one month and copy duplicate number 1
        long diff = 1000L*60*60*24*31;
        duplicate.setLastModified(duplicate.lastModified()-diff);
        org.copyAndOrganize(duplicate.getAbsolutePath());
        // go back an hour and copy duplicate number 2
        diff = 1000L*60*60;
        duplicate.setLastModified(duplicate.lastModified()-diff);
        org.copyAndOrganize(duplicate.getAbsolutePath());

        // the first duplicate should be in the file graph
        String path = search(new File(repoPath), "test2(1).txt");
        assertNotEquals(null, path);
        FileTools.delete(new File(path));
        // the second duplicate should also be in the file graph
        path = search(new File(repoPath), "test2(2).txt");
        assertNotEquals(null, path);

        // delete the test files and directories and update the file graph
        FileTools.delete(new File(path));
        fileGraph.update(fileGraph.getRoot());
    }

    /**
     * This is a helper function for finding files by their name starting from an initial starting point.
     * @param file file from which the search should start
     * @param fileName the file's name
     * @return null if there is no file with the given name in the subtree, else the file's absolute path as string
     */
    private String search(File file, String fileName) {
        if(file.isFile()) return file.getName().equals(fileName) ? file.getAbsolutePath() : null;
        for(File f : file.listFiles()) {
            String str = search(f, fileName);
            if(str != null) return str;
        }
        return null;
    }
}
