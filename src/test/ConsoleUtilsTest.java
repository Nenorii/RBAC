import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    @Test
    void promptStringRequired() {
        String input = "\nhello\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        String result = ConsoleUtils.promptString(scanner, "msg: ", true);
        assertEquals("hello", result);
    }

    @Test
    void promptIntInRange() {
        String input = "abc\n0\n3\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        int value = ConsoleUtils.promptInt(scanner, "num: ", 1, 5);
        assertEquals(3, value);
    }

    @Test
    void promptYesNo() {
        String input = "maybe\nда\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertTrue(ConsoleUtils.promptYesNo(scanner, "?: "));
    }

    @Test
    void promptChoice() {
        String input = "2\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        List<String> options = List.of("one", "two", "three");
        String choice = ConsoleUtils.promptChoice(scanner, "Выберите:", options);
        assertEquals("two", choice);
    }

    @AfterAll
    static void restoreInput() {
        System.setIn(System.in);
    }
}
