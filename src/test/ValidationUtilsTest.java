import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void validUsernames() {
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("admin_root"));
        assertTrue(ValidationUtils.isValidUsername("A1b2C3"));
    }

    @Test
    void invalidUsernames() {
        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("too_long_username_12345"));
        assertFalse(ValidationUtils.isValidUsername("bad name"));
        assertFalse(ValidationUtils.isValidUsername("name!"));
    }

    @Test
    void validEmails() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name+tag@sub.domain.org"));
    }

    @Test
    void invalidEmails() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail("not-an-email"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@@example.com"));
    }

    @Test
    void validDates() {
        assertTrue(ValidationUtils.isValidDate("2024-01-01"));
        assertTrue(ValidationUtils.isValidDate("1999-12-31"));
    }

    @Test
    void invalidDates() {
        assertFalse(ValidationUtils.isValidDate(null));
        assertFalse(ValidationUtils.isValidDate("2024/01/01"));
        assertFalse(ValidationUtils.isValidDate("24-01-01"));
        assertFalse(ValidationUtils.isValidDate("2024-1-1"));
    }

    @Test
    void normalizeStringTrimsAndCollapsesSpaces() {
        assertNull(ValidationUtils.normalizeString(null));
        assertEquals("hello world", ValidationUtils.normalizeString("  hello   world  "));
    }

    @Test
    void requireNonEmptyThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty(null, "field"));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty("   ", "field"));
    }

    @Test
    void requireNonEmptyPassesOnNonEmpty() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("value", "field"));
    }
}
