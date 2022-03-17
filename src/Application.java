import organizer.*;
import util.FileTools;

import java.awt.*;
import java.io.File;

public class Application {
    public static void main(String[] args) {
        File source = new File(args[0]);
        System.out.printf("file count: %d, size: %f\n", FileTools.count(source), FileTools.size(source)/1e6);

        Organizer org = new DefaultOrganizer();
        try {
            String errors = org.copyAndOrganize(args[0], args[1]);
            System.out.println(errors);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void defaultVsGraph(String source, String dest) {
        FileTools.clearToTrashbin(new File(dest + "/test1"));
        FileTools.clearToTrashbin(new File(dest + "/test2"));

        Organizer dOrg = new DefaultOrganizer(), gOrg = new GraphOrganizer();

        long start = System.nanoTime(), def = 0, graph = 0;
        try {
            String errors = dOrg.copyAndOrganize(source, dest + "/test1");
            def = System.nanoTime() - start;
            System.out.println(errors);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        start = System.nanoTime();
        try {
            String errors = gOrg.copyAndOrganize(source, dest + "/test2");
            graph = System.nanoTime() - start;
            System.out.println(errors);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.printf("default time: %f, graph time: %f\n", def / 1e6, graph / 1e6);
        System.out.printf("graph is %f%% faster than default\n", 100 * (def / (double) graph));
    }
}
