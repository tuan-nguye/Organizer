package com.org.util;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

public class FileTools {
    public static int count(File file) {
        return count(file, null);
    }

    public static int count(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(file.isFile()) return 1;

        int count = 0;
        for(File child : file.listFiles(filter)) {
            count += count(child);
        }

        return count;
    }

    public static int countDirectFiles(File file) {
        return countDirectFiles(file, null);
    }

    public static int countDirectFiles(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(!file.isDirectory()) return 1;

        int count = 0;
        for(File f : file.listFiles(filter)) {
            if(f.isFile()) count++;
        }

        return count;
    }

    public static long size(File file) {
        return size(file, null);
    }

    public static long size(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(file.isFile()) return file.length();

        long sum = 0;
        for(File child : file.listFiles(filter)) {
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
        if(file == null || !file.exists()) return;
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
        if(file == null || !file.exists()) return;
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

    /**
     * read properties from hidden property file
     * @return returns properties map, if the file doesn't exist it returns empty map
     */
    public static Properties readProperties(String path) {
        Properties properties = new Properties();
        File propertyFile = new File(path);

        try {
            FileInputStream in = new FileInputStream(propertyFile);
            properties.load(in);
            in.close();
        } catch(FileNotFoundException fnfe) {
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return properties;
    }

    /**
     * update and store new properties
     * @param properties
     * @param path
     */
    public static void storeProperties(Properties properties, String path) {
        FileWriter writer = null;

        try {
            File propertyFile = new File(path);
            setFileVisibility(propertyFile, true);
            writer = new FileWriter(propertyFile);
            properties.store(writer, "");
            setFileVisibility(propertyFile, false);
        } catch(IOException ioe) {
            System.err.println("error occurred writing property file");
            ioe.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch(IOException ioe) {}
        }
    }

    public static boolean setFileVisibility(File file, boolean visible) {
        try {
            Files.setAttribute(file.toPath(), "dos:hidden", !visible);
        } catch(IOException ioe) {
            return false;
        }

        return true;
    }

    public static LocalDateTime dateTime(long lastModified) {
        Instant instant = Instant.ofEpochMilli(lastModified);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime;
    }

    public static long epochMilli(LocalDateTime ldt) {
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        return fileName.substring(fileName.lastIndexOf('.')+1);
    }

    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.')+1);
    }

    public static String getNameWithoutPrefix(String prefix, String path) {
        if(path == prefix) return "";
        return path.substring(path.lastIndexOf(File.separator)+1);
    }
}
