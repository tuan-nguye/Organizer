package organizer;

import util.graph.FileDirectoryGraphDeprecated;
import util.FileTools;
import util.graph.FileGraphDeprecated;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

/*
file count: 14800, size: 8218,852865

default time: 104137,522600 ms, graph time: 158038,449500 ms
graph is 65,893789% faster than default

GraphOrganizer is slower than my first default implementation Sadeg
 */
public class YearMonthGraphOrganizer extends Organizer {
    private FileGraphDeprecated<String[], File> graph;

    @Override
    public String copyAndOrganize(String source, String destination) {
        graph = new FileDirectoryGraphDeprecated();
        graph.setRoot(destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source or destination is not a directory");

        count = 0;
        errors.setLength(0);
        dfs(root);

        return errors.toString();
    }

    private void dfs(File file) {
        if(file.isFile()) {
            copyFile(file);
            count++;
            notifyObservers();
            return;
        }

        for(File child : file.listFiles()) {
            dfs(child);
        }
    }

    protected Path getDirectory(Date date) {
        calendar.setTime(date);
        String year = String.valueOf(calendar.get(calendar.YEAR));
        String month = FileTools.folderName(months[calendar.get(calendar.MONTH)], year);
        String[] args = new String[] {year, month};

        if(!graph.contains(args)) graph.add(args);
        return graph.get(args).toPath();
    }
}
