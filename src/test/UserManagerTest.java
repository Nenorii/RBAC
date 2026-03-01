import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    UserManager manager;

    @BeforeEach
    void setUp() {
        manager = new UserManager();
    }

    @Test
    void addAndFindById() {
        var u = User.create("john_doe", "John Doe", "john@mail.com");
        manager.add(u);
        assertEquals(u, manager.findById("john_doe").orElse(null));
        assertEquals(1, manager.count());
    }

    @Test
    void addDuplicateThrows() {
        var u = User.create("john_doe", "John Doe", "john@mail.com");
        manager.add(u);
        assertThrows(IllegalArgumentException.class, () -> manager.add(u));
    }

    @Test
    void remove() {
        var u = User.create("john_doe", "John Doe", "john@mail.com");
        manager.add(u);
        assertTrue(manager.remove(u));
        assertTrue(manager.findById("john_doe").isEmpty());
    }

    @Test
    void findByUsername() {
        var u = User.create("jane_admin", "Jane Admin", "jane@mail.com");
        manager.add(u);
        assertEquals(u, manager.findByUsername("jane_admin").orElse(null));
    }

    @Test
    void findByEmail() {
        var u = User.create("bob", "Bob User", "bob@company.com");
        manager.add(u);
        assertEquals(u, manager.findByEmail("bob@company.com").orElse(null));
    }

    @Test
    void exists() {
        manager.add(User.create("alice", "Alice", "alice@mail.com"));
        assertTrue(manager.exists("alice"));
        assertFalse(manager.exists("bob"));
    }

    @Test
    void update() {
        manager.add(User.create("john", "John", "john@mail.com"));
        manager.update("john", "John Smith", "john.smith@mail.com");
        var u = manager.findById("john").orElseThrow();
        assertEquals("John Smith", u.fullName());
        assertEquals("john.smith@mail.com", u.email());
    }

    @Test
    void findByFilter() {
        manager.add(User.create("john_1", "John A", "john@a.com"));
        manager.add(User.create("jane_2", "Jane B", "jane@b.com"));
        var list = manager.findByFilter(UserFilters.byUsernameContains("john"));
        assertEquals(1, list.size());
        assertEquals("john_1", list.get(0).username());
    }

    @Test
    void clear() {
        manager.add(User.create("u1u", "U1", "u1@m.com"));
        manager.clear();
        assertEquals(0, manager.count());
    }
}