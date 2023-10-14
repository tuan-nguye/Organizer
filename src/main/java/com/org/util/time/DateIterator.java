package com.org.util.time;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DateIterator implements Iterator<String> {
    List<String> time = new ArrayList<>();
    private int idx = 0;

    public DateIterator(LocalDateTime dateTime) {
        time.add(String.valueOf(dateTime.getYear()));
        time.add(DateStats.monthStr[dateTime.getMonthValue()-1]);
        time.add(String.valueOf(dateTime.getDayOfMonth()));
        time.add(dateTime.getHour()+DateStats.unit[4]);
        time.add(dateTime.getMinute()+DateStats.unit[5]);
        time.add(dateTime.getSecond()+DateStats.unit[6]);
    }

    @Override
    public boolean hasNext() {
        return idx != time.size();
    }

    @Override
    public String next() {
        return time.get(idx++);
    }
}
