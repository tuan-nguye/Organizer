package util.time;

import java.util.HashMap;
import java.util.Map;

public class DateStats {
    public static final String[] monthStr = new String[] {"jan", "feb", "märz", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez"};
    public static final Map<String, Integer> monthInt = new HashMap<>();

    /**
     * year -> month -> day -> hour -> minute -> second
     * inclusive ranges for each time unit
     */
    public static final int[][] dateRange = new int[][] {
            {0, Integer.MAX_VALUE},
            {0, 11},
            {0, 31},    // not correct actually for feb, apr, jun, ...
            {0, 23},
            {0, 59},
            {0, 59}
    };

    public static final String[] unit = new String[] {"", "", "", "h", "min", "s"};

    static {
        for(int i = 0; i < monthStr.length; i++) monthInt.put(monthStr[i], i);
    }
}
