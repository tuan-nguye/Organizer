package com.org.util.time.format;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class JpgFormat implements FormatInterface {
    @Override
    public LocalDateTime readDate(File file) throws ImageProcessingException, IOException {
        LocalDateTime ldt = null;

        Metadata md = ImageMetadataReader.readMetadata(file);
        ExifSubIFDDirectory directory = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if(directory == null) return null;
        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
        if(date == null) date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME, TimeZone.getDefault());
        if(date == null) date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED, TimeZone.getDefault());
        if(date != null) ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        return ldt;
    }
}
