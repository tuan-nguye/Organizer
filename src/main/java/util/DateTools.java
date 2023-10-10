package util;

import util.time.DateIterator;

import java.io.File;

public class DateTools {
    public static String folderName(File file, int depth) {
        StringBuilder folderNameBuilder = new StringBuilder();
        DateIterator di = new DateIterator(FileTools.dateTime(file.lastModified()));
        boolean first = true;

        for(int i = 0; i < depth; i++) {
            if(first) first = false;
            else folderNameBuilder.append("_");
            folderNameBuilder.append(di.next());
        }

        return folderNameBuilder.toString();
    }
}