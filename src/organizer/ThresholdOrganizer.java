package organizer;

import util.graph.DynamicFileGraph;
import util.graph.FileGraph;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

public class ThresholdOrganizer extends Organizer {
    private FileGraph<Date, File> graph;

    public ThresholdOrganizer() {
    }

    @Override
    public String copyAndOrganize(String source, String destination) {
        graph = new DynamicFileGraph(destination);
        return null;
    }

    @Override
    protected Path getDirectory(Date date) {
        return null;
    }
}
