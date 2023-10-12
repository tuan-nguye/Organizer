package tests.classes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.FileTools;
import util.time.DateExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DateExtractorTest {
    private static final String repoPath = Path.of("test-bin/dateTest").toAbsolutePath().toString();

    @BeforeAll
    public static void prepare() {
        File repo = new File(repoPath);
        repo.mkdirs();
    }

    @Test
    public void jpgTest() {
        File jpg = getResourceAsFile("example_files/test_img.jpg");

        System.out.println(DateExtractor.getDate(jpg));
        LocalDateTime ldtExtr = DateExtractor.getDate(jpg);
        LocalDateTime ldtCorrect = LocalDateTime.of(2021, 12, 20, 10, 36, 7);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void mp4Test() {
        File mp4 = getResourceAsFile("example_files/video3.mp4");
        LocalDateTime ldtExtr = DateExtractor.getDate(mp4);
        LocalDateTime ldtCorrect = LocalDateTime.of(2018, 10, 10, 22, 57, 31);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void defaultTest() {
        File mp4 = getResourceAsFile("example_files/video3.mp4");
        LocalDateTime ldtLastModified = FileTools.dateTime(mp4.lastModified());
        LocalDateTime ldtCorrect = LocalDateTime.of(2021, 10, 15, 11, 24, 22);
    }

    private File copy(String resource) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resource);

        String fileName = FileTools.getNameWithoutPrefix("", resource);
        String outputPath = repoPath + File.separator + fileName;
        File file = new File(outputPath);
        if (inputStream != null) {
            // Define the output file path where you want to save the file
            /*
            try {
                file.createNewFile();
            } catch(IOException ioe) {
                System.err.println(ioe.getMessage());
            }*/

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
                int bytesRead;
                byte[] buffer = new byte[1024];

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                System.out.println("File saved to: " + outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Resource not found: " + resource);
        }

        return file;
    }

    private File getResourceAsFile(String resource) {
        URL url = getClass().getClassLoader().getResource(resource);
        File file = null;

        try {
            file = new File(url.toURI());
        } catch(Exception e) {
            System.err.println("getResourceAsFile failed: " + e.getMessage());
        }

        return file;
    }
}
