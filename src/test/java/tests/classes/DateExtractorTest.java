package tests.classes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.org.organizer.Organizer;
import com.org.organizer.ThresholdOrganizer;
import com.org.organizer.copy.Copy;
import org.junit.jupiter.api.Test;
import com.org.util.FileTools;
import com.org.util.time.DateExtractor;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test class for testing DateExtractor functionality
 */
public class DateExtractorTest {
    // path to the repository to test in
    private static final String TEST_FILES_DIR = "res/example_files";

    /**
     * print all files while recursively iterating through the filesystem tree.
     * helper function for debugging and testing
     * @param file
     */
    private void print(File file) {
        if(file.isFile()) System.out.println(file);
        else for(File f : file.listFiles()) print(f);
    }

    /**
     * print all directories from the metadata object from the metadata-extractor
     * library. helper function for debugging and testing
     * @param file
     */
    private void printMetadata(File file) {
        try {
            Metadata md = ImageMetadataReader.readMetadata(file);
            for(Directory d : md.getDirectories()) {
                for(Tag t : d.getTags()) {
                    System.out.println(t.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("epic fail: " + e.getMessage());
        }
    }

    /**
     * Test that the correct date is read from the jpg file. As jpg files have datetime
     * metadata, it shouldn't read the lastModified field.
     */
    @Test
    public void jpgTest() {
        File img = new File(TEST_FILES_DIR, "img.jpg");
        LocalDateTime ldtExtr = DateExtractor.getDate(img);
        // milliseconds are ignored and used for marking the file
        ldtExtr = ldtExtr.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2021, 12, 20, 10, 36, 7);
        assertEquals(ldtCorrect, ldtExtr);
    }

    /**
     * Test that the correct date is read from the mp4 file. As mp4 files have datetime
     * metadata, it shouldn't read the lastModified field.
     */
    @Test
    public void mp4Test() {
        File mp4 = new File(TEST_FILES_DIR, "vid.mp4");
        LocalDateTime ldtExtr = DateExtractor.getDate(mp4);
        ldtExtr = ldtExtr.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2018, 10, 10, 22, 57, 31);
        assertEquals(ldtCorrect, ldtExtr);
    }

    /**
     * Unsupported file extensions don't have any datetime metadata to read from. So
     * the lastModified field will be used instead.
     */
    @Test
    public void defaultTest() {
        File txt = new File("test-bin", "text.txt");
        LocalDateTime ldtCorrect = LocalDateTime.of(2023, 10, 13, 14, 56, 8);
        // create the file and set the last modified field
        try {
            txt.getParentFile().mkdirs();
            txt.createNewFile();
            txt.setLastModified(FileTools.epochMilli(ldtCorrect));
        } catch(Exception e) {
            fail(e.getMessage());
        }
        // check for equality without the milliseconds
        LocalDateTime ldtLastModified = DateExtractor.getDate(txt);
        ldtLastModified = ldtLastModified.truncatedTo(ChronoUnit.SECONDS);
        assertEquals(ldtCorrect, ldtLastModified);
    }

    /**
     * Test the extraction of the date on a corrupted jpg file. It should return null.
     */
    @Test
    public void testNull() {
        File corruptJpg = new File("test-bin", "image.jpg");
        // create a corrupt jpg file without any data
        try {
            corruptJpg.getParentFile().mkdirs();
            corruptJpg.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }
        // getDate should return null
        LocalDateTime ldt = DateExtractor.getDate(corruptJpg);
        assertNull(ldt);
    }

    /**
     * After the ThresholdOrganizer copies or moves files to their correct location
     * in the repository, they should be marked, so future date extractions can be
     * performed much faster. This test checks, that the files are marked in the
     * file graph structure destination.
     */
    @Test
    public void testMark() {
        // create the example txt file and a repository
        File txt = new File("test-bin", "text.txt");
        File repo = new File("test-bin" + File.separator + "extrRepo");
        // create the txt file set static date in the lastModified field
        // if not set, the date is always the current date and changes every time the test is executed
        try {
            txt.getParentFile().mkdirs();
            txt.createNewFile();
            LocalDateTime ldt = LocalDateTime.of(2023, 10, 25, 16, 46, 30, 0);
            txt.setLastModified(FileTools.epochMilli(ldt));
        } catch(Exception e) {
            fail(e.getMessage());
        }
        // create the folder for the repository root
        repo.mkdirs();
        // organizer with copy operatoin
        Organizer org = new ThresholdOrganizer(new Copy(), 5, repo.getAbsolutePath());
        org.copyAndOrganize(txt.getAbsolutePath());
        // change the txt file's date, so they are not seen as identical
        txt.setLastModified(txt.lastModified()-1000);
        org.copyAndOrganize(txt.getAbsolutePath());

        // check that they both files are marked in the repository
        File txtNewLocation = new File(repo.getAbsolutePath() + File.separator + "2023", txt.getName());
        assertTrue(DateExtractor.fileIsMarked(txtNewLocation));
        File txt1NewLocation = new File(txtNewLocation.getParent(), "text(1).txt");
        assertTrue(DateExtractor.fileIsMarked(txt1NewLocation));

        // clean up
        FileTools.delete(repo);
    }

    /**
     * Test the date extraction function from a file name. The date is parsed in the file's
     * name in the format yyyy-mm-dd-hh-mm-ss.
     */
    @Test
    public void testDateFromFileName() {
        // file name from a real example. all metadata was removed from the screenshot
        // and instead added in the name for whatever reason
        String fileName = "Screenshot_2022-02-05-05-42-25-713_com.miui.gallery.jpg";
        LocalDateTime ldtExtr = DateExtractor.getDate(fileName).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2022, 2, 5, 5, 42, 25);
        assertEquals(ldtCorrect, ldtExtr);
    }
}
