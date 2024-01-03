package tests.classes;

import org.junit.jupiter.api.Test;
import com.org.util.time.DateIterator;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class tests the DateIterator functionality.
 */
public class DateIteratorTest {
    /**
     * This test makes sure that the iterator returns the correct time units and
     * in the right order.
     */
    @Test
    public void correctTime() {
        // create an example date
        int year = 2023, month = 3, day = 20, hour = 10, min = 51, sec = 10;
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, min, sec);
        DateIterator it = new DateIterator(dateTime);

        // test that the iterator returns the correct time
        assertEquals(year, Integer.parseInt(it.next()));
        assertEquals("märz", it.next());
        assertEquals(day, Integer.parseInt(it.next()));
        assertEquals(hour+"h", it.next());
        assertEquals(min+"min", it.next());
        assertEquals(sec+"s", it.next());
    }
}
