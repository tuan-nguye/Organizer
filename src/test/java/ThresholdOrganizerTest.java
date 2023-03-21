import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import organizer.Organizer;
import organizer.ThresholdOrganizer;
import organizer.copy.Copy;
import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThresholdOrganizerTest {
    private static final String repoPath = Path.of("test-bin/repo").toAbsolutePath().toString();
    private static final String testFilesPath = "test-bin/exampleFiles";

    /*
    example files dynamically generated:
    in order: 21.03.2023 09:25:10, 03.02.2023 02:31:30, 17.07.2010 19:24:53, 17.03.2023 22:13:03, 19.08.2021 17:32:03
     */
    private static final long[] exampleFileTimes = new long[] {1679387110238l, 1675387890214l, 1279387493013l, 1679087583401l, 1629387123456l};

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

        File testIn = new File(testFilesPath);
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
    }

    @Test
    public void test() {
        Organizer organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.copyAndOrganize(testFilesPath, repoPath);
    }
}
