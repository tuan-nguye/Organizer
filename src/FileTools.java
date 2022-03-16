import java.io.File;

public class FileTools {
    public static int count(File file) {
        if(file.isFile()) return 1;

        int count = 0;
        for(File child : file.listFiles()) {
            count += count(child);
        }

        return count;
    }

    public static long size(File file) {
        if(file.isFile()) return file.length();

        long sum = 0;
        for(File child : file.listFiles()) {
            sum += size(child);
        }

        return sum;
    }
}
