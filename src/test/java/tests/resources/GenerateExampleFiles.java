package tests.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class offers five example txt files with static lastModified fields for testing reasons. They are always created
 * in the same directory and can be used from there
 */
public class GenerateExampleFiles {
    // directory path to the test files
    public static final String testFilesPath = "test-bin/exampleFiles";

    /**
    example txt files dynamically generated:
    in order:
    21.03.2023 09:25:10,
    03.02.2023 02:31:30,
    17.07.2010 19:24:53,
    17.03.2023 22:13:03,
    19.08.2021 17:32:03
     */
    public static final long[] exampleFileTimes = new long[] {1679387110238l, 1675387890214l, 1279387493013l, 1679087583401l, 1629387123456l};
    // stores whether the example files have been created already
    private static boolean initialized = false;

    /**
     * This function will create five txt files and assign them the static datetime attributes in the lastModified field.
     * The files will only be created once. Subsequent calls will skip the creation.
     * @return if the files have been created within the call of the function, true if yes, false if they already exist
     */
    public static boolean generate() {
        // if the example files already exist return
        if(initialized) return false;

        // create the all txt files in a separate directory
        File testIn = new File(testFilesPath, "txt");
        if(!testIn.exists()) testIn.mkdirs();

        // create the txt files and write some input
        for(int i = 0; i < exampleFileTimes.length; i++) {
            File exampleFile = new File(testIn, "test"+i+".txt");
            try {
                exampleFile.createNewFile();
                FileWriter fileWriter = new FileWriter(exampleFile);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                // write i times the dot char into the file
                buffWriter.write(".".repeat(i));
                buffWriter.close();
                fileWriter.close();
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            exampleFile.setLastModified(exampleFileTimes[i]);
        }

        // create a single csv file in its separate directory
        File csvDir = new File(testFilesPath, "csv");
        csvDir.mkdir();
        File csv = new File(csvDir, "testCsv.csv");

        try {
            csv.createNewFile();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        initialized = true;
        return true;
    }
}
