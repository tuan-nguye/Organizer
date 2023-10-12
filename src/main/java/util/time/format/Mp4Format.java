package util.time.format;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class Mp4Format implements FormatInterface {
    @Override
    public LocalDateTime readDate(File file) {
        LocalDateTime ldt = null;

        try {
            Metadata md = ImageMetadataReader.readMetadata(file);
            Mp4Directory directory = md.getFirstDirectoryOfType(Mp4Directory.class);
            Date date = directory.getDate(Mp4Directory.TAG_CREATION_TIME, TimeZone.getDefault());
            if(date == null) date = directory.getDate(Mp4Directory.TAG_MODIFICATION_TIME, TimeZone.getDefault());
            ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        } catch(Exception e) {
            System.err.println("failed to read date from mp4 file: " + e.getMessage());
        }

        return ldt;
    }
}
