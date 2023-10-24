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

public class ThresholdOrganizerTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();

    private static ThresholdOrganizer organizer;
    private static FileGraph fileGraph;

    @BeforeAll
    public static void prepare() {
        Configuration conf = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, conf, 1);

        organizer = new ThresholdOrganizer(new CopyReplace(), 1, repoPath);
        organizer.fileExtensionAllowed("txt");
        fileGraph = FileGraphFactory.get(repoPath);
    }

    @Test
    public void testCopyAndOrganize() {
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"txt");

        String[] files = new String[] {
                "test-bin/repo/2010/test2.txt",
                "test-bin/repo/2021/test4.txt",
                "test-bin/repo/2023/2023_feb/test1.txt",
                "test-bin/repo/2023/2023_märz/2023_märz_17/test3.txt",
                "test-bin/repo/2023/2023_märz/2023_märz_21/test0.txt"
        };

        for(String fileStr : files) {
            File file = new File(fileStr);
            assertTrue(file.exists());
        }

        assertEquals(files.length+1, FileTools.countFiles(new File(repoPath)));
    }

    @Test
    public void addUnallowedFileExtension() {
        organizer.allowFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"csv");
        assertEquals(0, FileTools.countFiles(new File(repoPath), (dir, name) -> name.contains("csv")));
    }

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

        organizer.allowFileExtension("jpg");
        organizer.copyAndOrganize(corruptJpg.getAbsolutePath());

        File jpgCorrectFolder = new File(repoPath + File.separator + Configuration.ERROR_FOLDER_NAME, "image.jpg");
        assertTrue(jpgCorrectFolder.exists());
        jpgCorrectFolder.delete();
        fileGraph.update(fileGraph.getRoot());
    }

    @Test
    public void duplicateReplaceTest() {
        File duplicateSrc = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new Copy();
        File duplicate = new File("test-bin", "test2.txt");
        Organizer orgCopy = new ThresholdOrganizer(copy, 2, repoPath);
        Organizer orgMove = new ThresholdOrganizer(new MoveReplace(), 2, repoPath);

        try {
            copy.execute(duplicateSrc.toPath(), duplicate.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        orgCopy.copyAndOrganize(duplicate.getAbsolutePath());
        orgMove.copyAndOrganize(duplicate.getAbsolutePath());
        assertFalse(duplicate.exists());
    }

    @Test
    public void duplicateRenameTest() {
        File duplicateSrc = new File(GenerateExampleFiles.testFilesPath + File.separator + "txt", "test2.txt");
        ICopy copy = new Copy();
        File duplicate = new File("test-bin", "test2.txt");
        Organizer org = new ThresholdOrganizer(copy, 3, repoPath);

        try {
            copy.execute(duplicateSrc.toPath(), duplicate.toPath());
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }

        // copy original
        org.copyAndOrganize(duplicateSrc.getAbsolutePath());
        // copy duplicate number 1
        long diff = 1000L*60*60*24*31;
        duplicate.setLastModified(duplicate.lastModified()-diff);
        org.copyAndOrganize(duplicate.getAbsolutePath());
        // copy duplicate number 2
        diff = 1000L*60*60;
        duplicate.setLastModified(duplicate.lastModified()-diff);
        org.copyAndOrganize(duplicate.getAbsolutePath());

        String path = search(new File(repoPath), "test2(1).txt");
        assertNotEquals(null, path);
        FileTools.delete(new File(path));

        path = search(new File(repoPath), "test2(2).txt");
        assertNotEquals(null, path);
        FileTools.delete(new File(path));

    }

    private String search(File file, String fileName) {
        if(file.isFile()) return file.getName().equals(fileName) ? file.getAbsolutePath() : null;
        for(File f : file.listFiles()) {
            String str = search(f, fileName);
            if(str != null) return str;
        }
        return null;
    }
}
