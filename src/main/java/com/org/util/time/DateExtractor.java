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

/**
 * The DateExtractor class offers some functionality for handling the datetime for files.
 * Such as, extracting the datetime metadata if there are any, and marking a file by storing
 * the extracted datetime in the lastModified field for quick access. For the extraction,
 * the metadata-extractor library (https://github.com/drewnoakes/metadata-extractor) is used.
 */
public class DateExtractor {
    // set of all supported file extensions as string
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
    // current year as the application is running
    private static final int currentYear = LocalDateTime.now().getYear();
    // can be set as true if all marks on the files should be ignored
    private static boolean ignoreMark = false;
    // map storing the tags which store the datetime for each directory class, library specific
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
     * read the date from a file, if it's a jpg, mp4, or any other supported file with date metadata
     * return this value. otherwise return the last modified date. If the file is marked
     * this function will instead return the lastModified field, as it's much faster.
     * @param file
     * @return the date associated to the file or last modified, can return
     * null if an error occurred, e.g. corrupt jpg file
     */
    public static LocalDateTime getDate(File file) {
        if(!file.exists() || !file.isFile()) return null;
        // if marks are allowed and the file is marked, then return the lastModified field
        if(!ignoreMark && fileIsMarked(file)) return FileTools.dateTime(file.lastModified());
        LocalDateTime ldt = null;
        String ext = FileTools.getFileExtension(file);
        // skip extraction if the file extension is not supported
        if(supportedFileExtensions.contains(ext)) {
            try {
                ldt = extractDateFromMetadata(file);
            } catch(Exception e) {
                return null;
            }
        }
        // sometimes the datetime is parsed in the file's name
        if(ldt == null) ldt = DateExtractor.getDate(file.getName());
        // if it reaches this point, return the lastModified, as every file supports this attribute
        if(ldt == null) ldt = FileTools.dateTime(file.lastModified());
        return ldt;
    }

    /**
     * Read the datetime attribute from a file and mark the file.
     * @param file file object
     */
    public static void markFile(File file) {
        LocalDateTime ldt = getDate(file);
        if(ldt == null) return;
        markFile(file, ldt);
    }

    /**
     * Mark the file with the given datetime. The datetime will be converted to epochMillis
     * and a hash created with the file's name and saved in the last three digits of the
     * decimal number. The lastModified field will be modified into the date given as input.
     * Since accessing this attribute is much faster than having to extract it from its metadata,
     * it will save a lot of time. The datetime in the lastModified filed will be correct up
     * until the milliseconds, as they are used for the hash.
     * @param file file object
     * @param ldt local datetime object
     */
    public static void markFile(File file, LocalDateTime ldt) {
        if(ldt == null) return;
        // the datetime for the lastModified field are given as milliseconds since 01.01.1970
        long epochMillis = FileTools.epochMilli(ldt);
        // create the hash from the file's name, the hash has a 1/1000 chance to be correct
        // even if the file isn't actually marked
        long fileHash = fileNameHashCode(file.getName());
        // only use the last 3 digits
        long mark = fileHash % 1000;
        // replace the last 3 digits of the datetime in long with the hash
        long markedLastModified = epochMillis - (epochMillis%1000) + mark;
        file.setLastModified(markedLastModified);
    }

    /**
     * If set to true, the mark in the file will be ignored. That means every time
     * a file's date is extracted using the getDate(File file) function, it will
     * read the file's metadata.
     * @param boolIgnoreMark
     */
    public static void setIgnoreMark(boolean boolIgnoreMark) {
        ignoreMark = boolIgnoreMark;
    }

    /**
     * Uses the metadata-extractor library functions to extract the metadata saved
     * inside a file.
     * @param file file object
     * @return the localdatetime stored in the file, can return null if nothing was found
     * @throws Exception
     */
    private static LocalDateTime extractDateFromMetadata(File file) throws Exception {
        Metadata md = readMetadata(file);
        // quick time formats (mov) are not read correctly
        boolean quickTime = false;
        if(md == null) {
            md = readQuickTimeMetadata(file);
            quickTime = true;
        }

        // iterate through all datetime attributes of the file and if multiple
        // are stored, then use the earliest possible date
        Date minDate = null;
        for(Directory dir : md.getDirectories()) {
            List<Integer> dateTags = dateTagMap.get(dir.getClass());
            if(dateTags == null) continue;
            for(int tag : dateTags) {
                Date date = dir.getDate(tag, TimeZone.getDefault());
                if(date == null) continue;
                LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                // sometimes the datetime from the file could be incorrect
                if(ldt.getYear() < 1970 || ldt.getYear() > currentYear) continue;
                // choose the earliest date
                if(minDate == null) minDate = date;
                else if(date.compareTo(minDate) < 0) minDate = date;
            }
        }
        // if no datetime field was found and both readMetadata() and the quicktime metadata
        // was attempted, the file is often corrupted
        if(minDate == null) {
            if(quickTime) throw new Exception("file might be corrupted");
            else return null;
        }
        // return the date converted to localdatetime, assumes that the application is run
        // at the same time zone (should possibly be set statically, or as repository property)
        return LocalDateTime.ofInstant(minDate.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Read the metadata of a file using the metadata-extractor library and return a
     * Metadata object storing all found directories.
     * @param file file object
     * @return metadata object, can be null
     */
    private static Metadata readMetadata(File file) {
        Metadata md = null;

        try {
            md = ImageMetadataReader.readMetadata(file);
        } catch(ImageProcessingException ipe) {
        } catch(IOException ioe) {
        }

        return md;
    }

    /**
     * Read the quicktime metadata of a file using the metadata-extractor library and
     * return a Metadata object storing all found directories.
     * @param file file object
     * @return metadat object, can be null
     */
    private static Metadata readQuickTimeMetadata(File file) {
        Metadata md = null;
        try {
            md = QuickTimeMetadataReader.readMetadata(file);
        } catch(ImageProcessingException ipe) {
        } catch(IOException ioe) {
        }
        return md;
    }

    /**
     * Check whether a file has been marked by comparing the last 3 digits of the lastModified
     * field with the hash of the file's name.
     * @param file file object
     * @return true if it has been marked, false otherwise
     */
    public static boolean fileIsMarked(File file) {
        long lm = file.lastModified();
        long lmMark = lm % 1000;
        long fileHash = fileNameHashCode(file.getName());
        long nameMark = fileHash % 1000;
        // if the file is marked the hash is the last 3 digits of the lastModified field
        return lmMark == nameMark;
    }

    /**
     * Create a hash code from a string value. The hash value is always positive. The hash
     * code for the same string is always the same.
     * @param value string input
     * @return a hash code as long
     */
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
        // if a date is stored in the name, its numbers are in the name, such as:
        // IMG_2022-08-05-16-38-25
        String[] nums = str.split("\\D+");
        List<String> timeUnits = new ArrayList<>();
        boolean first = true;
        int i = 0;
        // go through the string from left to right, the first number needs to have 4
        // letters for the year, all the others only 2
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
        // if there are not enough numbers, it probably isn't a date
        if(timeUnits.size() < 6) return null;
        // get the first 6 numbers for year, month, day, hour, min, second
        timeUnits = timeUnits.subList(0, 6);
        // convert to the list to a string using the default .toString() function
        String strDate = timeUnits.toString();
        // lists look like [...], so remove the brackets at the start and the end
        strDate = strDate.substring(1, strDate.length()-1);
        String pattern = "yyyy, MM, dd, HH, mm, ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        // try parsing the datetime, it can throw an exception if the date doesn't make sense
        LocalDateTime ldt = null;
        try {
            ldt = LocalDateTime.parse(strDate, formatter);
        } catch(Exception e) {
        }
        // if the year is above the current year, something must be wrong
        if(ldt != null && ldt.getYear() > currentYear) ldt = null;
        return ldt;
    }
}
