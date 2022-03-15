import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Date;

public class DefaultOrganizer implements Organizer {
    private Calendar calendar;
    private Path dest;
    private String[] months;

    public DefaultOrganizer() {
        calendar = Calendar.getInstance();
        months = new String[] {"jan", "feb", "maerz", "apr", "mai", "juni", "juli", "aug",
        "sept", "okt", "nov", "dez"};
    }

    @Override
    public void copyAndOrganize(String source, String destination) {
        System.out.printf("source = %s, dest = %s\n", source, destination);
        File root = new File(source);
        if(!root.isDirectory() || !(new File(destination)).isDirectory())
            throw new IllegalArgumentException("source is not a directory");

        this.dest = Path.of(destination);
        dfs(root);
    }

    private void dfs(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                dfs(f);
            } else {
                copyFile(f);
            }
        }
    }

    private boolean copyFile(File f) {
        Date date = new Date(f.lastModified());
        calendar.setTime(date);
        Path path = dest.resolve(Path.of(String.valueOf(calendar.get(calendar.YEAR)), months[calendar.get(calendar.MONTH)]));

        try {
            Files.createDirectories(path);
            Files.copy(f.toPath(), path.resolve(f.getName()), StandardCopyOption.COPY_ATTRIBUTES);
        } catch(Exception e) {
            System.out.printf("failed to copy file: %s, from %s to %s\n", f.getName(), f.toPath().toString(), path.toString());
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }
}
