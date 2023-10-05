package tests.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;
import resources.GenerateExampleFiles;
import resources.InitializeTestRepository;
import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThresholdOrganizerTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();

    private static ThresholdOrganizer organizer;

    @BeforeAll
    public static void prepare() {
        Configuration conf = new Configuration();
        InitializeTestRepository.generateRepository(repoPath, conf, 1);

        organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.allowExtension("txt");
    }

    @Test
    public void testCopyAndOrganize() {
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"txt", repoPath);

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

        assertEquals(files.length+1, FileTools.count(new File(repoPath)));
    }

    @Test
    public void addUnallowedFileExtension() {
        organizer.addFileExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath+File.separator+"csv", repoPath);
        assertEquals(0, FileTools.count(new File(repoPath), (dir, name) -> name.contains("csv")));
    }
}
