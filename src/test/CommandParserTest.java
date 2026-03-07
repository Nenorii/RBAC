import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    CommandParser parser;
    RBACSystem system;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();
    }

    @Test
    void registerAndExecuteCommand() {
        final boolean[] executed = {false};
        parser.registerCommand("test", "Test command", (scanner, sys) -> executed[0] = true);

        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        parser.executeCommand("test", scanner, system);
        assertTrue(executed[0]);
    }

    @Test
    void executeUnknownCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("unknown_cmd", scanner, system));
    }

    @Test
    void parseAndExecute() {
        final String[] result = {""};
        parser.registerCommand("hello", "Say hello", (scanner, sys) -> result[0] = "executed");

        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        parser.parseAndExecute("hello", scanner, system);
        assertEquals("executed", result[0]);
    }

    @Test
    void parseAndExecuteCaseInsensitive() {
        final String[] result = {""};
        parser.registerCommand("hello", "Say hello", (scanner, sys) -> result[0] = "executed");

        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        parser.parseAndExecute("HELLO", scanner, system);
        assertEquals("executed", result[0]);
    }

    @Test
    void printHelp() {
        parser.registerCommand("cmd1", "Description 1", (s, sys) -> {});
        parser.registerCommand("cmd2", "Description 2", (s, sys) -> {});
        assertDoesNotThrow(() -> parser.printHelp());
    }

    @Test
    void parseEmptyInput() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.parseAndExecute("", scanner, system));
        assertDoesNotThrow(() -> parser.parseAndExecute("   ", scanner, system));
    }

    @Test
    void commandThrowingException() {
        parser.registerCommand("error", "Error command", (scanner, sys) -> {
            throw new RuntimeException("Test error");
        });

        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));

        assertThrows(RuntimeException.class, () ->
                parser.parseAndExecute("error", scanner, system));
    }


}