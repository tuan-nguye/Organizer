package com.org.util;

import com.org.util.time.DateExtractor;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

public class FileTools {
    public static int countFiles(File file) {
        return countFiles(file, (FileFilter) null);
    }

    public static int countFiles(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(file.isFile()) {
            if(filter == null || filter.accept(file.getParentFile(), file.getName())) return 1;
            else return 0;
        }

        int count = 0;
        for(File f : file.listFiles()) {
            count += countFiles(f, filter);
        }

        return count;
    }

    public static int countFiles(File file, FileFilter filter) {
        if(file == null) return 0;
        else if(file.isFile()) {
            if(filter == null || filter.accept(file)) return 1;
            else return 0;
        }

        int count = 0;
        for(File f : file.listFiles()) {
            count += countFiles(f, filter);
        }

        return count;
    }

    public static int countFolders(File folder) {
        return countFolders(folder, (FileFilter) null);
    }

    public static int countFolders(File folder, FileFilter filter) {
        if(folder == null || !folder.exists()) return 0;
        else if(folder.isFile()) return 0;

        int count = 0;
        if(filter == null || filter.accept(folder)) count = 1;
        for(File f : folder.listFiles(a -> a.isDirectory())) {
            count += countFolders(f, filter);
        }

        return count;
    }

    private static int countFolders(File folder, FilenameFilter filter) {
        if(folder == null || !folder.exists()) return 0;
        else if(folder.isFile()) return 0;

        int count = 0;
        if(filter == null || filter.accept(folder.getParentFile(), folder.getName())) count = 1;
        for(File f : folder.listFiles(a -> a.isDirectory())) {
            count += countFolders(f, filter);
        }

        return count;
    }

    public static int countDirectFiles(File file) {
        return countDirectFiles(file, null);
    }

    public static int countDirectFiles(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(!file.isDirectory()) return 1;

        File[] files = file.listFiles(filter);
        int count = files != null ? files.length : 0;

        return count;
    }

    public static long size(File file) {
        return size(file, null);
    }

    public static long size(File file, FilenameFilter filter) {
        if(file == null) return 0;
        else if(file.isFile()) {
            if(filter == null || filter.accept(file.getParentFile(), file.getName())) return file.length();
            else return 0;
        }
        long sum = 0;
        for(File f : file.listFiles()) {
            sum += size(f, filter);
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
        if(ldt == null) return 0;
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        int idxDot = fileName.lastIndexOf('.');
        if(idxDot == -1) return "";
        return fileName.substring(idxDot+1).toLowerCase();
    }

    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.')+1);
    }

    public static String getNameWithoutPrefix(String prefix, String path) {
        if(path.equals(prefix)) return "";
        return path.substring(path.lastIndexOf(File.separator)+1);
    }

    public static String chooseFileName(String path, String fileName, LocalDateTime originalTime) {
        File file = new File(path, fileName);
        LocalDateTime ldt = DateExtractor.getDate(file);

        if(!file.exists() || (ldt == null && originalTime == null) || ldt.truncatedTo(ChronoUnit.SECONDS).equals(originalTime.truncatedTo(ChronoUnit.SECONDS))) return fileName;

        int idxDot = fileName.lastIndexOf('.');
        String name = fileName.substring(0, idxDot);
        String ext = fileName.substring(idxDot+1);

        StringBuilder nameBuilder = new StringBuilder();
        int count = 1;
        while(true) {
            nameBuilder.append(name).append('(').append(count).append(')').append('.').append(ext);
            file = new File(path, nameBuilder.toString());
            if(file.exists()) ldt = DateExtractor.getDate(file);
            else break;
            if(ldt.equals(originalTime)) break;
            nameBuilder.setLength(0);
            count++;
        }

        return nameBuilder.toString();
    }
}
