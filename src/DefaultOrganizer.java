import java.io.File;
import java.util.Calendar;
import java.util.Date;

public class DefaultOrganizer implements Organizer {
    private Calendar calendar;

    public DefaultOrganizer() {
        calendar = Calendar.getInstance();
    }

    @Override
    public void copyAndOrganize(String source, String destination) {
        System.out.printf("source = %s, dest = %s\n", source, destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source is not a directory");

        dfs(root);
    }

    private void dfs(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                dfs(f);
            } else {
                Date d = new Date(f.lastModified());
                System.out.printf("name: %s, date: %s", f.getName(), d);
                calendar.setTime(d);
                System.out.printf(", month: %s\n", calendar.get(calendar.MONTH));
            }
        }
    }
}
