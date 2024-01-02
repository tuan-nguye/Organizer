package com.org.util.time;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The iterator turns a LocalDateTime object into an iterator returning the single time units
 * from large to small, from year down to the seconds.
 */
public class DateIterator implements Iterator<String> {
    // time unit list
    List<String> time = new ArrayList<>();
    // current index in the time list
    private int idx = 0;

    /**
     * Date iterator constructor. It takes in the time and extracts the time units
     * and stores them sorted in a list. The order is: year, month (as string), day
     * of month, hours, minutes, seconds
     * @param dateTime
     */
    public DateIterator(LocalDateTime dateTime) {
        time.add(String.valueOf(dateTime.getYear()));
        time.add(DateStats.monthStr[dateTime.getMonthValue()-1]);
        time.add(String.valueOf(dateTime.getDayOfMonth()));
        time.add(dateTime.getHour()+DateStats.unit[4]);
        time.add(dateTime.getMinute()+DateStats.unit[5]);
        time.add(dateTime.getSecond()+DateStats.unit[6]);
    }

    /**
     * Returns true if there still are elements in the iterator.
     * @return
     */
    @Override
    public boolean hasNext() {
        return idx != time.size();
    }

    /**
     * Returns the next element in the iterator and increments it.
     * @return
     */
    @Override
    public String next() {
        return time.get(idx++);
    }
}
