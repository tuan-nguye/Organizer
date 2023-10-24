package tests.classes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.junit.jupiter.api.Test;
import com.org.util.FileTools;
import com.org.util.time.DateExtractor;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.util.Date;
import java.util.TimeZone;

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
        ldtExtr = LocalDateTime.of(ldtExtr.getYear(), ldtExtr.getMonth(), ldtExtr.getDayOfMonth(), ldtExtr.getHour(), ldtExtr.getMinute(), ldtExtr.getSecond());
        LocalDateTime ldtCorrect = LocalDateTime.of(2021, 12, 20, 10, 36, 7);
        assertEquals(ldtCorrect, ldtExtr);
    }

    @Test
    public void mp4Test() {
        File mp4 = new File(TEST_FILES_DIR, "vid.mp4");
        LocalDateTime ldtExtr = DateExtractor.getDate(mp4);
        ldtExtr = LocalDateTime.of(ldtExtr.getYear(), ldtExtr.getMonth(), ldtExtr.getDayOfMonth(), ldtExtr.getHour(), ldtExtr.getMinute(), ldtExtr.getSecond());
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
        ldtLastModified = LocalDateTime.of(ldtLastModified.getYear(), ldtLastModified.getMonth(), ldtLastModified.getDayOfMonth(), ldtLastModified.getHour(), ldtLastModified.getMinute(), ldtLastModified.getSecond());
        assertEquals(ldtCorrect, ldtLastModified);
    }

    @Test
    public void testNull() {
        File corruptJpg = new File("test-bin", "image.jpg");

        try {
            corruptJpg.createNewFile();
        } catch(Exception e) {
            fail(e.getMessage());
        }

        LocalDateTime ldt = DateExtractor.getDate(corruptJpg);
        assertNull(ldt);
    }
}
