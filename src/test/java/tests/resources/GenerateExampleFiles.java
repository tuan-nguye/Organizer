package tests.resources;

import java.io.File;
import java.io.IOException;

public class GenerateExampleFiles {
    public static final String testFilesPath = "test-bin/exampleFiles";

    /*
    example txt files dynamically generated:
    in order:
    21.03.2023 09:25:10,
    03.02.2023 02:31:30,
    17.07.2010 19:24:53,
    17.03.2023 22:13:03,
    19.08.2021 17:32:03
     */
    private static final long[] exampleFileTimes = new long[] {1679387110238l, 1675387890214l, 1279387493013l, 1679087583401l, 1629387123456l};

    private static boolean initialized = false;

    public static boolean generate() {
        if(initialized) return false;

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

        initialized = true;
        return true;
    }
}
