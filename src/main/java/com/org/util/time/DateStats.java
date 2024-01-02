package com.org.util.time;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores information about the time units and dates, such as the month names
 * as string (only supports german for now), the valid ranges for each time unit, and
 * the unit strings.
 */
public class DateStats {
    // month string values
    public static final String[] monthStr = new String[] {"jan", "feb", "märz", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez"};
    // map storing the index value to access the monthStr array
    public static final Map<String, Integer> monthInt = new HashMap<>();

    /**
     * year -> month -> day -> hour -> minute -> second
     * inclusive ranges for each time unit
     */
    public static final int[][] dateRange = new int[][] {
            {Integer.MIN_VALUE, Integer.MAX_VALUE},
            {0, Integer.MAX_VALUE},
            {0, 11},
            {0, 31},    // not correct actually for feb, apr, jun, ...
            {0, 23},
            {0, 59},
            {0, 59}
    };

    // saves the units for each time phase
    public static final String[] unit = new String[] {"", "", "", "", "h", "min", "s"};

    // add the index values to the index map
    static {
        for(int i = 0; i < monthStr.length; i++) monthInt.put(monthStr[i], i);
    }
}
