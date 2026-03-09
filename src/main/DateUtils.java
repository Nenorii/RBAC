import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(FORMATTER);
    }
    private DateUtils() {}

    public static String now() {
        return getCurrentDateTime();
    }

    public static LocalDateTime parseExpiry(String expiryStr) {
        try {
            return LocalDateTime.parse(expiryStr, FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(expiryStr + " 23:59", FORMATTER);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException(
                        "Неверный формат даты: " + expiryStr +
                                ". Ожидается: yyyy-MM-dd HH:mm");
            }
        }
    }

    public static boolean isExpired(String expiryStr) {
        if (expiryStr == null) return false;

        LocalDateTime expiry = parseExpiry(expiryStr);
        return expiry.isBefore(LocalDateTime.now());
    }

    public static long daysUntil(String expiryStr) {
        if (expiryStr == null) return -1;

        LocalDateTime expiry = parseExpiry(expiryStr);
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expiry);
    }

    public static String format(String dateStr) {
        if (dateStr == null) return "постоянно";

        try {
            LocalDateTime dt = parseExpiry(dateStr);
            return dt.format(FORMATTER);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static boolean isFutureDate(String dateStr) {
        try {
            LocalDateTime dt = parseExpiry(dateStr);
            return dt.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    public static String extend(String currentExpiry, int days) {
        if (currentExpiry == null) {
            return LocalDateTime.now()
                    .plusDays(days)
                    .format(FORMATTER);
        }

        LocalDateTime expiry = parseExpiry(currentExpiry);
        return expiry.plusDays(days).format(FORMATTER);
    }
}
