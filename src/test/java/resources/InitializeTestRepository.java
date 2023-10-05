package resources;

import parser.CommandException;
import parser.Configuration;
import parser.command.Command;
import parser.command.InitializeRepository;

import java.io.File;

public class InitializeTestRepository {
    public static void generateRepository(String repoPath, Configuration config, int threshold) {
        File testOut = new File(repoPath);
        if(!testOut.exists()) testOut.mkdirs();
        Configuration.PROPERTY_FILE_PATH_STRING = repoPath;
        Command initRepo = new InitializeRepository();

        try {
            initRepo.execute(new String[] {Integer.toString(threshold)}, config);
        } catch(CommandException ce) {
            System.err.println(ce.getMessage());
        }

        GenerateExampleFiles.generate();
    }
}
