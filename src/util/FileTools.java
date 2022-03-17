package util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTools {
    public static int count(File file) {
        if(file == null) return 0;
        else if(file.isFile()) return 1;

        int count = 0;
        for(File child : file.listFiles()) {
            count += count(child);
        }

        return count;
    }

    public static long size(File file) {
        if(file == null) return 0;
        else if(file.isFile()) return file.length();

        long sum = 0;
        for(File child : file.listFiles()) {
            sum += size(child);
        }

        return sum;
    }

    // clear the directory and deletes all files terminally, preserves root directory, files are NOT moved to the trash bin
    // USE AT OWN RISK peepoSHAKE
    public static void clear(File root) {
        if(root == null || !root.isDirectory()) return;

        for(File child : root.listFiles())
            delete(child);
    }

    // clear the directory and deletes all files terminally but preserves directories, files are NOT moved to the trash bin
    // USE AT OWN RISK peepoSHAKE
    public static void clearFilesOnly(File file) {
        if(file == null) return;
        else if(file.isFile()) {
            file.delete();
            return;
        }

        for(File child : file.listFiles())
            clearFilesOnly(child);
    }

    // recursively deletes all files and directories, root directory also deleted, files are NOT moved to the trash bin
    // USE AT OWN RISK peepoSHAKE
    public static void delete(File file) {
        if(file == null) return;
        else if(file.isFile()) {
            file.delete();
            return;
        }

        for(File child : file.listFiles())
            delete(child);

        file.delete();
    }

    // moves all files and subdirectories into trashbin, preserves root, can be restored
    public static void clearToTrashbin(File root) {
        if(root == null || !root.isDirectory()) return;

        for(File child : root.listFiles())
            trashbin(child);
    }

    // moves whole directory into the trash bin, can be restored
    public static void trashbin(File file) {
        Desktop.getDesktop().moveToTrash(file);
    }

    public static String folderName(String month, String year) {
        return month + "_" + year;
    }
}
