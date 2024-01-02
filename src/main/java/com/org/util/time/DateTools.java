package com.org.util.time;

import java.io.File;

/**
 * Class that offers some useful functions concerning dates.
 */
public class DateTools {
    /**
     * This function reads the file's datetime attribute and returns the appropriate
     * folder name according to the filegraph standard.
     * @param file the file object
     * @param depth the depth in which the node is
     * @return folder name as string
     */
    public static String folderName(File file, int depth) {
        StringBuilder folderNameBuilder = new StringBuilder();
        DateIterator di = new DateIterator(DateExtractor.getDate(file));
        boolean first = true;

        for(int i = 0; i < depth; i++) {
            // time units are separated via underscores, add if not the first element
            if(first) first = false;
            else folderNameBuilder.append("_");
            folderNameBuilder.append(di.next());
        }

        return folderNameBuilder.toString();
    }
}
