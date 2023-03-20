import org.junit.jupiter.api.Test;
import util.time.DateIterator;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DateIteratorTest {
    @Test
    public void correctTime() {
        int year = 2023, month = 2, day = 20, hour = 10, min = 51, sec = 10;
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, min, sec);
        DateIterator it = new DateIterator(dateTime);

        assertEquals(year, Integer.parseInt(it.next()));
        assertEquals("märz", it.next());
        assertEquals(day, Integer.parseInt(it.next()));
        assertEquals(hour+"h", it.next());
        assertEquals(min+"min", it.next());
        assertEquals(sec+"s", it.next());
    }
}
