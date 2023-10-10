package util.graph;

import java.util.HashMap;
import java.util.Map;

public class FileGraphFactory {
    private static Map<String, FileGraph> fileGraphMap = new HashMap<>();

    public static FileGraph get(String path) {
        if(!fileGraphMap.containsKey(path)) fileGraphMap.put(path, new FileGraph(path));
        return fileGraphMap.get(path);
    }
}
