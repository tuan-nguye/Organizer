package organizer;

import util.graph.DynamicFileGraphDeprecated;
import util.graph.FileGraphDeprecated;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

public class ThresholdOrganizer extends Organizer {
    private FileGraphDeprecated<Date, String> graph;

    public ThresholdOrganizer(int threshold) {
        graph = new DynamicFileGraphDeprecated(threshold);
    }

    @Override
    public String copyAndOrganize(String source, String destination) {
        graph.setRoot(destination);
        dfs(new File(source));

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

    @Override
    protected Path getDirectory(Date date) {
        Path dir = Path.of(graph.get(date));
        errors.append(graph.getErrors());
        return dir;
    }
}
