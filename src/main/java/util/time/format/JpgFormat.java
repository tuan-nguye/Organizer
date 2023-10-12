package util.time.format;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class JpgFormat implements FormatInterface {
    @Override
    public LocalDateTime readDate(File file) {
        LocalDateTime ldt = null;

        try {
            Metadata md = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
            if(date == null) date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME, TimeZone.getDefault());
            if(date == null) date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED, TimeZone.getDefault());
            ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            System.err.println("failed to read date from jpg file: " + e.getMessage());
        }

        return ldt;
    }
}
