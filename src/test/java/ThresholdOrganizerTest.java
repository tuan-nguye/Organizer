import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;
import util.FileTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThresholdOrganizerTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();
    private static final String testFilesPath = "test-bin/exampleFiles";

    /*
    example files dynamically generated:
    in order: 21.03.2023 09:25:10, 03.02.2023 02:31:30, 17.07.2010 19:24:53, 17.03.2023 22:13:03, 19.08.2021 17:32:03
     */
    private static final long[] exampleFileTimes = new long[] {1679387110238l, 1675387890214l, 1279387493013l, 1679087583401l, 1629387123456l};

    private static ThresholdOrganizer organizer;

    @BeforeAll
    public static void prepare() {
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        Configuration.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();
        Configuration conf = new Configuration();
        try {
            initRepo.execute(new String[] {String.valueOf(1)}, conf);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

        File testIn = new File(testFilesPath, "txt");
        if(!testIn.exists()) testIn.mkdirs();

        for(int i = 0; i < exampleFileTimes.length; i++) {
            File exampleFile = new File(testIn, "test"+i+".txt");
            try {
                exampleFile.createNewFile();
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            exampleFile.setLastModified(exampleFileTimes[i]);
        }

        File csvDir = new File(testFilesPath, "csv");
        csvDir.mkdir();
        File csv = new File(csvDir, "testCsv.csv");
        try {
            csv.createNewFile();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.allowExtension("txt");
    }

    @Test
    public void test() {
        organizer.copyAndOrganize(testFilesPath+File.separator+"txt", repoPath);

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
        organizer.copyAndOrganize(testFilesPath+File.separator+"csv", repoPath);
        assertEquals(0, FileTools.count(new File(repoPath), (dir, name) -> name.contains("csv")));
    }
}
