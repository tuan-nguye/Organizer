package util.time;


import util.FileTools;
import util.time.format.FormatInterface;
import util.time.format.JpgFormat;
import util.time.format.Mp4Format;

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
     public static LocalDateTime getDate(File file) {
         if(!file.exists() || !file.isFile()) return null;
         String fileType = FileTools.getFileExtension(file);
         LocalDateTime ldt = null;

         if(extractorMap.containsKey(fileType)) ldt = extractorMap.get(fileType).readDate(file);
         if(ldt == null) ldt = FileTools.dateTime(file.lastModified());

         return ldt;
     }
}
