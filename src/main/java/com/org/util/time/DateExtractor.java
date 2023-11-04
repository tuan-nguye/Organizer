package com.org.util.time;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.quicktime.QuickTimeMetadataReader;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.StringValue;
import com.drew.metadata.Tag;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.eps.EpsDirectory;
import com.drew.metadata.exif.*;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.wav.WavDirectory;
import com.org.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final int currentYear = LocalDateTime.now().getYear();

    private static boolean ignoreMark = false;

    private static Map<Class, List<Integer>> dateTagMap = new HashMap<>();

    static {
        dateTagMap.put(AviDirectory.class, List.of(AviDirectory.TAG_DATETIME_ORIGINAL));
        dateTagMap.put(EpsDirectory.class, List.of(EpsDirectory.TAG_MODIFY_DATE, EpsDirectory.TAG_CREATION_DATE));
        dateTagMap.put(ExifIFD0Directory.class, List.of(ExifIFD0Directory.TAG_DATETIME, ExifIFD0Directory.TAG_DATETIME_ORIGINAL, ExifIFD0Directory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(ExifImageDirectory.class, List.of(ExifImageDirectory.TAG_DATETIME, ExifImageDirectory.TAG_DATETIME_ORIGINAL, ExifImageDirectory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(ExifInteropDirectory.class, List.of(ExifInteropDirectory.TAG_DATETIME, ExifInteropDirectory.TAG_DATETIME_ORIGINAL, ExifInteropDirectory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(ExifSubIFDDirectory.class, List.of(ExifSubIFDDirectory.TAG_DATETIME, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(ExifThumbnailDirectory.class, List.of(ExifThumbnailDirectory.TAG_DATETIME, ExifThumbnailDirectory.TAG_DATETIME_ORIGINAL, ExifThumbnailDirectory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(GpsDirectory.class, List.of(GpsDirectory.TAG_DATETIME, GpsDirectory.TAG_DATETIME_ORIGINAL, GpsDirectory.TAG_DATETIME_DIGITIZED));
        dateTagMap.put(IptcDirectory.class, List.of(IptcDirectory.TAG_DATE_SENT, IptcDirectory.TAG_DATE_CREATED, IptcDirectory.TAG_RELEASE_DATE, IptcDirectory.TAG_REFERENCE_DATE, IptcDirectory.TAG_EXPIRATION_DATE, IptcDirectory.TAG_DIGITAL_DATE_CREATED));
        dateTagMap.put(QuickTimeDirectory.class, List.of(QuickTimeDirectory.TAG_CREATION_TIME, QuickTimeDirectory.TAG_MODIFICATION_TIME));
        dateTagMap.put(Mp4Directory.class, List.of(Mp4Directory.TAG_CREATION_TIME, Mp4Directory.TAG_MODIFICATION_TIME));
        dateTagMap.put(PngDirectory.class, List.of(PngDirectory.TAG_LAST_MODIFICATION_TIME));
        dateTagMap.put(WavDirectory.class, List.of(WavDirectory.TAG_DATE_CREATED));
    }

    /**
     * read the date from a file, if it's a jpg or mp4 file with date metadata
     * return this value. otherwise return the last modified date
     * @param file
     * @return the date associated to the file or last modified, can return
     * null if an error occurred, e.g. corrupt jpg file
     */
    public static LocalDateTime getDate(File file) {
        if(!file.exists() || !file.isFile()) return null;
        if(!ignoreMark && fileIsMarked(file)) return FileTools.dateTime(file.lastModified());
        LocalDateTime ldt = null;
        String ext = FileTools.getFileExtension(file);

        if(supportedFileExtensions.contains(ext)) {
            try {
                ldt = extractDateFromMetadata(file);
            } catch(Exception e) {
                return null;
            }
        }

        if(ldt == null) ldt = DateExtractor.getDate(file.getName());
        if(ldt == null) ldt = FileTools.dateTime(file.lastModified());
        return ldt;
    }

    public static void markFile(File file) {
        LocalDateTime ldt = getDate(file);
        if(ldt == null) return;
        markFile(file, ldt);
    }

    public static void markFile(File file, LocalDateTime ldt) {
        if(ldt == null) return;
        long epochMillis = FileTools.epochMilli(ldt);
        long fileHash = fileNameHashCode(file.getName());
        long mark = fileHash % 1000;
        long markedLastModified = epochMillis - (epochMillis%1000) + mark;
        file.setLastModified(markedLastModified);
    }

    public static void setIgnoreMark(boolean boolIgnoreMark) {
        ignoreMark = boolIgnoreMark;
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
            List<Integer> dateTags = dateTagMap.get(dir.getClass());
            if(dateTags == null) continue;
            for(int tag : dateTags) {
                Date date = dir.getDate(tag, TimeZone.getDefault());
                if(date == null) continue;
                LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                if(ldt.getYear() < 1970 || ldt.getYear() > currentYear) continue;

                if(minDate == null) minDate = date;
                else if(date.compareTo(minDate) < 0) minDate = date;
            }
        }

        if(minDate == null) {
            if(quickTime) throw new Exception("file might be corrupted");
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

    public static boolean fileIsMarked(File file) {
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

    /**
     * attempts to parse a localdatetime from an input string
     * only supports the pattern 'yyyy MM dd HH mm ss', if all
     * these values are in the right order and contained in the
     * name
     * @param str
     * @return
     */
    public static LocalDateTime getDate(String str) {
        String[] nums = str.split("\\D+");
        List<String> timeUnits = new ArrayList<>();
        boolean first = true;
        int i = 0;
        while(i < nums.length) {
            if(first) {
                if(nums[i].length() < 4) {
                    i++;
                } else {
                    timeUnits.add(nums[i].substring(0, 4));
                    nums[i] = nums[i].substring(4);
                    first = false;
                }
            } else {
                if(nums[i].length() < 2) {
                    i++;
                } else {
                    timeUnits.add(nums[i].substring(0, 2));
                    nums[i] = nums[i].substring(2);
                }

            }
        }
        if(timeUnits.size() < 6) return null;
        timeUnits = timeUnits.subList(0, 6);
        String strDate = timeUnits.toString();
        strDate = strDate.substring(1, strDate.length()-1);
        String pattern = "yyyy, MM, dd, HH, mm, ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        LocalDateTime ldt = null;
        try {
            ldt = LocalDateTime.parse(strDate, formatter);
        } catch(Exception e) {
            //System.err.println("error during parsing: " + e.getMessage());
        }

        if(ldt != null && ldt.getYear() > currentYear) ldt = null;
        return ldt;
    }
}
