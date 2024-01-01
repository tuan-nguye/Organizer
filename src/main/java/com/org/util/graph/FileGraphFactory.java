package com.org.util.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory offering static functions to return the same file graph references to string
 * paths. This makes sure, that only a single FileGraph exists for one path.
 */
public class FileGraphFactory {
    // map storing string paths mapped to file graphs
    private static Map<String, FileGraph> fileGraphMap = new HashMap<>();

    /**
     * Get the file graph created for the given string path. If it doesn't exist yet,
     * create a new one.
     * @param path
     * @return
     */
    public static FileGraph get(String path) {
        if(!fileGraphMap.containsKey(path)) fileGraphMap.put(path, new FileGraph(path));
        return fileGraphMap.get(path);
    }
}
