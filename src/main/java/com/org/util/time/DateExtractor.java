package com.org.util.time;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.quicktime.QuickTimeMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.org.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class DateExtractor {
    private static Set<String> supportedFileExtensions = new HashSet<String>(
            Arrays.asList(
                    "3fr",
                    "3g2",
                    "3gp",
                    "ai",
                    "arw",
                    "avi",
                    "bmp",
                    "cam",
                    "cr2",
                    "cr3",
                    "crw",
                    "dcr",
                    "dng",
                    "eps",
                    "fuzzed",
                    "gif",
                    "gpr",
                    "heic",
                    "heif",
                    "ico",
                    "j2c",
                    "jp2",
                    "jpeg",
                    "jpf",
                    "jpg",
                    "jpm",
                    "kdc",
                    "m2ts",
                    "m2v",
                    "m4a",
                    "m4v",
                    "mj2",
                    "mov",
                    "mp3",
                    "mp4",
                    "mpg",
                    "mts",
                    "nef",
                    "orf",
                    "pbm",
                    "pcx",
                    "pef",
                    "pgm",
                    "png",
                    "pnm",
                    "ppm",
                    "psd",
                    "raf",
                    "rw2",
                    "rwl",
                    "srw",
                    "tif",
                    "tiff",
                    "wav",
                    "webp",
                    "x3f"));
    /**
     * read the date from a file, if it's a jpg or mp4 file with date metadata
     * return this value. otherwise return the last modified date
     * @param file
     * @return the date associated to the file or last modified, can return
     * null if an error occurred, e.g. corrupt jpg file
     */
    public static LocalDateTime getDate(File file) {
        if(!file.exists() || !file.isFile()) return null;
        if(fileIsMarked(file)) return FileTools.dateTime(file.lastModified());
        LocalDateTime ldt = null;
        String ext = FileTools.getFileExtension(file);

        if(supportedFileExtensions.contains(ext)) {
            try {
                ldt = extractDateFromMetadata(file);
            } catch(Exception e) {
                return null;
            }
        }

        if(ldt == null) ldt = FileTools.dateTime(file.lastModified());
        markFile(file, ldt);
        ldt = FileTools.dateTime(file.lastModified());
        return ldt;
    }

    private static LocalDateTime extractDateFromMetadata(File file) throws Exception {
        Metadata md = readMetadata(file);
        boolean quickTime = false;
        if(md == null) {
            md = readQuickTimeMetadata(file);
            quickTime = true;
        }

        Date minDate = null;
        for(Directory dir : md.getDirectories()) {
            if(dir.getName().equals("File")) continue;
            for(Tag tag : dir.getTags()) {
                if(tag.getDescription().length() < 16) continue;
                Date date = dir.getDate(tag.getTagType(), TimeZone.getDefault());
                if(date != null) {
                    if(minDate == null) minDate = date;
                    else if(date.compareTo(minDate) < 0) minDate = date;
                }
            }
        }

        if(minDate == null) {
            if(quickTime) throw new Exception("file is probably corrupted");
            else return null;
        }
        return LocalDateTime.ofInstant(minDate.toInstant(), ZoneId.systemDefault());
    }

    private static Metadata readMetadata(File file) {
        Metadata md = null;

        try {
            md = ImageMetadataReader.readMetadata(file);
        } catch(ImageProcessingException ipe) {
        } catch(IOException ioe) {
        }

        return md;
    }

    private static Metadata readQuickTimeMetadata(File file) {
        Metadata md = null;
        try {
            md = QuickTimeMetadataReader.readMetadata(file);
        } catch(ImageProcessingException ipe) {
        } catch(IOException ioe) {
        }
        return md;
    }

    private static boolean fileIsMarked(File file) {
        long lm = file.lastModified();
        long lmMark = lm % 1000;
        long fileHash = fileNameHashCode(file.getName());
        long nameMark = fileHash % 1000;
        return lmMark == nameMark;
    }

    private static long fileNameHashCode(String value) {
        long h = 0;

        char val[] = value.toCharArray();

        if(val.length == 0) return 0;
        for (int i = 0; i < val.length; i++) {
            h = 31 * h + val[i];
        }

        if(h < 0) {
            if(h == Long.MIN_VALUE) h = Long.MAX_VALUE;
            else h = -h;
        }

        return h;
    }

    private static void markFile(File file, LocalDateTime ldt) {
        long epochMillis = FileTools.epochMilli(ldt);
        long fileHash = fileNameHashCode(file.getName());
        long mark = fileHash % 1000;
        long markedLastModified = epochMillis - (epochMillis%1000) + mark;
        file.setLastModified(markedLastModified);
    }
}
