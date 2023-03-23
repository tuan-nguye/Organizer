package util.time;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DateIterator implements Iterator<String> {
    String[] months = new String[] {"jan", "feb", "märz", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez"};
    List<String> time = new ArrayList<>();
    private int idx = 0;

    public DateIterator(LocalDateTime dateTime) {
        time.add(String.valueOf(dateTime.getYear()));
        time.add(months[dateTime.getMonthValue()-1]);
        time.add(String.valueOf(dateTime.getDayOfMonth()));
        time.add(dateTime.getHour()+"h");
        time.add(dateTime.getMinute()+"min");
        time.add(dateTime.getSecond()+"s");
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
