import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$"); // можно подстроить под твои требования

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private ValidationUtils() {
    }

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String normalized = username.trim();
        return USERNAME_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim();
        return EMAIL_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }
        String normalized = date.trim();
        return DATE_PATTERN.matcher(normalized).matches();
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        String singleSpaced = trimmed.replaceAll("\\s+", " ");
        return singleSpaced;
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Поле '" + fieldName + "' не может быть пустым");
        }
    }
}
