package tests.classes;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.quicktime.QuickTimeMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataReader;
import com.drew.metadata.Tag;
import com.org.util.time.DateExtractor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

public class TestTest {
    //@Test
    public void test() {
        File jpg = new File("E:\\User\\Documents\\programmieren\\projects\\Organizer\\res\\example_files\\img.jpg");
        jpg.setLastModified(jpg.lastModified()+1010*60);
        LocalDateTime ldt = DateExtractor.getDate(jpg);
        System.out.println(ldt);
    }

    private Date date(File file) {
        Date minDate = null;
        try {
            Metadata md = ImageMetadataReader.readMetadata(file);
            for(Directory dir : md.getDirectories()) {
                for(Tag tag : dir.getTags()) {
                    if(tag.getDescription().length() < 16) continue;
                    Date date = dir.getDate(tag.getTagType(), TimeZone.getDefault());
                    if(date != null) {
                        if(minDate == null) minDate = date;
                        else if(date.compareTo(minDate) < 0) minDate = date;
                    }
                }
            }
        } catch(ImageProcessingException ipe) {
            System.err.println(ipe.getMessage());
            try {
                Metadata md = QuickTimeMetadataReader.readMetadata(file);
                for(Directory dir : md.getDirectories()) {
                    for(Tag tag : dir.getTags()) {
                        if(tag.getDescription().length() < 16) continue;
                        Date date = dir.getDate(tag.getTagType(), TimeZone.getDefault());
                        if(date != null) {
                            if(minDate == null) minDate = date;
                            else if(date.compareTo(minDate) < 0) minDate = date;
                        }
                    }
                }
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        } catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            try {
                Metadata md = QuickTimeMetadataReader.readMetadata(file);
                for(Directory dir : md.getDirectories()) {
                    for(Tag tag : dir.getTags()) {
                        if(tag.getDescription().length() < 16) continue;
                        Date date = dir.getDate(tag.getTagType(), TimeZone.getDefault());
                        if(date != null) {
                            if(minDate == null) minDate = date;
                            else if(date.compareTo(minDate) < 0) minDate = date;
                        }
                    }
                }
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }

        return minDate;
    }

    private void printMetadata(File file) {
        try {
            Metadata md = ImageMetadataReader.readMetadata(file);
            for(Directory dir : md.getDirectories()) {
                System.out.println(dir.getName());
                for(Tag tag : dir.getTags()) {
                    System.out.println(tag);
                }
            }
        } catch(ImageProcessingException ipe) {
            System.err.println(ipe.getMessage());
            try {
                Metadata md = QuickTimeMetadataReader.readMetadata(file);
                for(Directory dir : md.getDirectories()) {
                    System.out.println(dir.getName());
                    for(Tag tag : dir.getTags()) {
                        System.out.println(tag);
                    }
                }
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        } catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            try {
                Metadata md = QuickTimeMetadataReader.readMetadata(file);
                for(Directory dir : md.getDirectories()) {
                    System.out.println(dir.getName());
                    for(Tag tag : dir.getTags()) {
                        System.out.println(tag);
                    }
                }
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
