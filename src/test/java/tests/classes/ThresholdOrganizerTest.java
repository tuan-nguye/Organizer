package tests.classes;

import com.org.util.time.DateExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.Copy;
import com.org.parser.Configuration;
import tests.resources.GenerateExampleFiles;
import tests.resources.InitializeTestRepository;
import com.org.util.FileTools;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThresholdOrganizerTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();

    private static ThresholdOrganizer organizer;

    @BeforeAll
    public static void prepare() {
        Configuration conf = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, conf, 1);

        organizer = new ThresholdOrganizer(new Copy(), 1, repoPath);
        organizer.fileExtensionAllowed("txt");
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
    }
}
