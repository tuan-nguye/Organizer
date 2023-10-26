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

public class DateExtractorTest {
    private static final String TEST_FILES_DIR = "res/example_files";

    private void print(File file) {
        if(file.isFile()) System.out.println(file);
        else for(File f : file.listFiles()) print(f);
    }

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

    @Test
    public void jpgTest() {
        File img = new File(TEST_FILES_DIR, "img.jpg");
        LocalDateTime ldtExtr = DateExtractor.getDate(img);
        ldtExtr = ldtExtr.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2021, 12, 20, 10, 36, 7);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void mp4Test() {
        File mp4 = new File(TEST_FILES_DIR, "vid.mp4");
        LocalDateTime ldtExtr = DateExtractor.getDate(mp4);
        ldtExtr = ldtExtr.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2018, 10, 10, 22, 57, 31);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void defaultTest() {
        File txt = new File("test-bin", "text.txt");
        LocalDateTime ldtCorrect = LocalDateTime.of(2023, 10, 13, 14, 56, 8);

        try {
            txt.getParentFile().mkdirs();
            txt.createNewFile();
            txt.setLastModified(FileTools.epochMilli(ldtCorrect));
        } catch(Exception e) {
            fail(e.getMessage());
        }

        LocalDateTime ldtLastModified = DateExtractor.getDate(txt);
        ldtLastModified = ldtLastModified.truncatedTo(ChronoUnit.SECONDS);
        assertEquals(ldtCorrect, ldtLastModified);
    }

    @Test
    public void testNull() {
        File corruptJpg = new File("test-bin", "image.jpg");

        try {
            corruptJpg.getParentFile().mkdirs();
            corruptJpg.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        LocalDateTime ldt = DateExtractor.getDate(corruptJpg);
        assertNull(ldt);
    }

    @Test
    public void testMark() {
        File txt = new File("test-bin", "text.txt");
        File repo = new File("test-bin" + File.separator + "extrRepo");

        try {
            txt.getParentFile().mkdirs();
            txt.createNewFile();
            LocalDateTime ldt = LocalDateTime.of(2023, 10, 25, 16, 46, 30, 0);
            txt.setLastModified(FileTools.epochMilli(ldt));
        } catch(Exception e) {
            fail(e.getMessage());
        }

        repo.mkdirs();
        Organizer org = new ThresholdOrganizer(new Copy(), 5, repo.getAbsolutePath());
        org.copyAndOrganize(txt.getAbsolutePath());
        txt.setLastModified(txt.lastModified()-1000);
        org.copyAndOrganize(txt.getAbsolutePath());

        // check
        File jpgNewLocation = new File(repo.getAbsolutePath() + File.separator + "2023", txt.getName());
        assertTrue(DateExtractor.fileIsMarked(jpgNewLocation));
        File jpg1NewLocation = new File(jpgNewLocation.getParent(), "text(1).txt");
        assertTrue(DateExtractor.fileIsMarked(jpg1NewLocation));

        // clean up
        FileTools.delete(repo);
    }

    @Test
    public void testDateFromFileName() {
        String fileName = "Screenshot_2022-02-05-05-42-25-713_com.miui.gallery";
        LocalDateTime ldtExtr = DateExtractor.getDate(fileName).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ldtCorrect = LocalDateTime.of(2022, 2, 5, 5, 42, 25);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void test() {
        File file = new File("E:\\backup\\fotos_mama\\unsortiert\\2023_feb_20");
        dfs(file);
    }

    private void dfs(File file) {
        if(file.isFile()) {
            System.out.println(DateExtractor.fileIsMarked(file) + ", " + DateExtractor.getDate(file));
        } else {
            for(File f : file.listFiles()) {
                dfs(f);
            }
        }
    }
}
