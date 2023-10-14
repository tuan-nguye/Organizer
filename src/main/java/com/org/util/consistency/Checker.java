package com.org.util.consistency;

import com.org.parser.Configuration;

import java.io.File;

/*
refactor pre-/postcondition checks
 */
public class Checker {
    public static void checkNotNull(Object o) {
        if(o == null) throw new IllegalArgumentException("object " + o + "is null");
    }

    public static void checkStringNotEmpty(String str) {
        if(str.isEmpty()) throw new IllegalArgumentException("string can't be empty");
    }

    public static boolean validRepository(String path) {
        File repo;
        if(path.isEmpty()) repo = new File(Configuration.PROPERTY_FILE_NAME_STRING);
        else repo = new File(path, Configuration.PROPERTY_FILE_NAME_STRING);
        return repo.exists();
    }
}
