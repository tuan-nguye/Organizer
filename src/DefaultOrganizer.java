import java.io.File;

public class DefaultOrganizer implements Organizer {
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
                System.out.println(f.getName());
            }
        }
    }
}
