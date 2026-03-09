import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void getCurrentDateTime() {
        String now = DateUtils.getCurrentDateTime();
        assertTrue(now.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
        assertEquals(16, now.length());
    }

    @Test
    void parseExpiry_validFormat() {
        LocalDateTime result = DateUtils.parseExpiry("2027-12-31 23:59");
        assertEquals(2027, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(31, result.getDayOfMonth());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
    }

    @Test
    void parseExpiry_dateOnly() {
        LocalDateTime result = DateUtils.parseExpiry("2027-12-31");
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
    }

    @Test
    void isExpired_future() {
        assertFalse(DateUtils.isExpired("2027-12-31 23:59"));
    }

    @Test
    void isExpired_past() {
        assertTrue(DateUtils.isExpired("2026-01-01 00:00"));
    }

    @Test
    void format_permanent() {
        assertEquals("постоянно", DateUtils.format(null));
    }

    @Test
    void format_validDate() {
        String result = DateUtils.format("2027-12-31 23:59");
        assertEquals("2027-12-31 23:59", result);
    }

    @Test
    void isFutureDate() {
        assertTrue(DateUtils.isFutureDate("2027-12-31 23:59"));
        assertFalse(DateUtils.isFutureDate("2026-01-01 00:00"));
    }

    @Test
    void extend_permanent() {
        String result = DateUtils.extend(null, 30);
        LocalDateTime dt = DateUtils.parseExpiry(result);
        assertTrue(dt.isAfter(LocalDateTime.now()));
    }

    @Test
    void extend_existing() {
        String result = DateUtils.extend("2027-12-31 23:59", 30);
        LocalDateTime dt = DateUtils.parseExpiry(result);
        assertTrue(dt.getDayOfMonth() >= 30);
    }

    @Test
    void daysUntil_future() {
        long days = DateUtils.daysUntil("2027-12-31 23:59");
        assertTrue(days > 0);
    }

    @Test
    void daysUntil_permanent() {
        assertEquals(-1, DateUtils.daysUntil(null));
    }
}
