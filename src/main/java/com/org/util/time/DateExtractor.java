package com.org.util.time;


import com.org.util.FileTools;
import com.org.util.time.format.FormatInterface;
import com.org.util.time.format.JpgFormat;
import com.org.util.time.format.Mp4Format;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DateExtractor {
    private static Map<String, FormatInterface> extractorMap = new HashMap<>();

    static {
        FormatInterface fi = new JpgFormat();
        extractorMap.put("jpg", fi);
        extractorMap.put("jpeg", fi);
        extractorMap.put("mp4", new Mp4Format());
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
     String fileType = FileTools.getFileExtension(file);
     LocalDateTime ldt = null;

     if(extractorMap.containsKey(fileType)) ldt = extractorMap.get(fileType).readDate(file);
     if(ldt == null) ldt = FileTools.dateTime(file.lastModified());

     return ldt;
    }
}
