import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void formatTable_simple() {
        String[] headers = {"Name", "Age"};
        List<String[]> rows = List.of(
                new String[]{"Alice", "25"},
                new String[]{"Bob", "30"}
        );
        String result = FormatUtils.formatTable(headers, rows);

        // Проверяем наличие ключевых элементов, а не точное совпадение
        assertTrue(result.contains("Name"));
        assertTrue(result.contains("Age"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("25"));
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("30"));
        assertTrue(result.startsWith("+"));
        assertTrue(result.contains("|"));
        assertTrue(result.endsWith("+\n"));
    }

    @Test
    void formatHeader_simple() {
        String result = FormatUtils.formatHeader("Users");
        assertTrue(result.contains("Users"));
        assertTrue(result.contains("="));
        assertTrue(result.startsWith("+"));
        assertTrue(result.contains("|"));
    }

    @Test
    void formatTable_empty() {
        String[] headers = {"Name"};
        List<String[]> rows = List.of();
        String result = FormatUtils.formatTable(headers, rows);
        assertEquals("Пустая таблица", result);
    }

    @Test
    void formatBox_simple() {
        String result = FormatUtils.formatBox("Hello World");
        assertTrue(result.contains("| Hello World |"));
        assertTrue(result.startsWith("+"));
        assertTrue(result.endsWith("+\n"));
    }

    @Test
    void truncate_shortText() {
        String result = FormatUtils.truncate("short", 10);
        assertEquals("short", result);
    }

    @Test
    void padRight() {
        String result = FormatUtils.padRight("test", 10);
        assertEquals("test      ", result);
        assertEquals(10, result.length());
    }

    @Test
    void padLeft() {
        String result = FormatUtils.padLeft("test", 10);
        assertEquals("      test", result);
        assertEquals(10, result.length());
    }
}
