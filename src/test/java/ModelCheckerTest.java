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
import util.consistency.ModelChecker;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;

public class ModelCheckerTest {
    private static final String repoPath = Path.of("test-bin/repoModelCheck").toAbsolutePath().toString();
    private static FileGraph graph;
    private static ModelChecker checker;

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

        Organizer organizer = new ThresholdOrganizer(new Copy(), 1);
        organizer.allowExtension("txt");
        organizer.copyAndOrganize(GenerateExampleFiles.testFilesPath, repoPath);

        graph = new FileGraph(repoPath);
        checker = new ModelChecker(conf);
    }

    @Test
    public void testCheckFile() {

    }
}
