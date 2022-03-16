import organizer.DefaultOrganizer;
import organizer.Organizer;
import util.FileTools;

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
}
