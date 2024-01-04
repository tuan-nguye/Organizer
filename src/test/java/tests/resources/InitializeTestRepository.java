package tests.resources;

import com.org.parser.command.CommandException;
import com.org.parser.Configuration;
import com.org.parser.command.Command;
import com.org.parser.command.InitializeRepository;

import java.io.File;

/**
 * This class initializes a test repository in a given directory. This makes it easy to set up an empty repository
 * reusing the command object.
 */
public class InitializeTestRepository {
    /**
     * Generate a repository in a given directory with a certain threshold. Set the repository path in the configuration
     * object and generate the test files if they didn't exist yet.
     * @param repoPath
     * @param config
     * @param threshold
     */
    public static void generateRepository(String repoPath, Configuration config, int threshold) {
        // the folder has to exist if a repository needs to be initialized there
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        // store the path of the repo in the config object
        config.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();

        try {
            // simulate the command by passing the threshold in as a string
            initRepo.execute(new String[] {Integer.toString(threshold)}, config);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

        // prepare the example files so you don't need to do that afterwards
        // after calling this function the example files can be copied and organized into the repo without having to call
        // this function again
        GenerateExampleFiles.generate();
    }
}
