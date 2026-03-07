import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    CommandParser parser;
    RBACSystem system;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();
        CommandRegistry.registerAllCommands(parser);
    }

    @Test
    void userListCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("user-list", scanner, system));
    }

    @Test
    void userCreateCommand() {
        String input = "test_user\nTest User\ntest@example.com\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("user-create", scanner, system));
        assertTrue(system.getUserManager().exists("test_user"));
    }

    @Test
    void userViewCommand() {
        String input = "admin\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("user-view", scanner, system));
    }

    @Test
    void userUpdateCommand() {
        String input = "admin\nNew Admin Name\nadmin@newsystem.local\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("user-update", scanner, system));

        var admin = system.getUserManager().findByUsername("admin").orElseThrow();
        assertEquals("New Admin Name", admin.fullName());
    }

    @Test
    void roleListCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("role-list", scanner, system));
    }

    @Test
    void roleViewCommand() {
        String input = "Admin\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("role-view", scanner, system));
    }

    @Test
    void roleCreateCommand() {
        String input = "TestRole\nTest Description\nнет\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("role-create", scanner, system));
        assertTrue(system.getRoleManager().exists("TestRole"));
    }

    @Test
    void assignRoleCommand() {
        system.getUserManager().add(User.create("bob", "Bob User", "bob@test.com"));
        String input = "bob\n2\n1\nTest reason\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("assign-role", scanner, system));
    }

    @Test
    void assignmentListCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("assignment-list", scanner, system));
    }

    @Test
    void assignmentListUserCommand() {
        String input = "admin\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("assignment-list-user", scanner, system));
    }

    @Test
    void assignmentActiveCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("assignment-active", scanner, system));
    }

    @Test
    void assignmentExpiredCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("assignment-expired", scanner, system));
    }

    @Test
    void permissionsUserCommand() {
        String input = "admin\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("permissions-user", scanner, system));
    }

    @Test
    void permissionsCheckCommand() {
        String input = "admin\nREAD\nusers\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("permissions-check", scanner, system));
    }

    @Test
    void helpCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("help", scanner, system));
    }

    @Test
    void statsCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("stats", scanner, system));
    }

    @Test
    void clearCommand() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes()));
        assertDoesNotThrow(() -> parser.executeCommand("clear", scanner, system));
    }
}