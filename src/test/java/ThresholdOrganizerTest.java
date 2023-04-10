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
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        Configuration.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();
        Configuration conf = new Configuration();

        try {
            initRepo.execute(new String[] {"1"}, conf);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

        GenerateExampleFiles.generate();

        organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.allowExtension("txt");
    }

    @Test
    public void test() {
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
