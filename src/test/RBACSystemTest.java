import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {

    RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
    }

    @Test
    void initialize() {
        system.initialize();
        assertTrue(system.getUserManager().count() > 0);
        assertTrue(system.getRoleManager().count() >= 3);
        assertTrue(system.getAssignmentManager().count() > 0);

        var admin = system.getUserManager().findByUsername("admin");
        assertTrue(admin.isPresent());

        var adminRole = system.getRoleManager().findByName("Admin");
        assertTrue(adminRole.isPresent());
        assertFalse(adminRole.get().getPermissions().isEmpty());
    }

    @Test
    void getCurrentUser() {
        system.setCurrentUser("test_user");
        assertEquals("test_user", system.getCurrentUser());
    }

    @Test
    void getCurrentUserDefault() {
        assertNull(system.getCurrentUser()); // ← более реалистично
    }


    @Test
    void generateStatistics() {
        system.initialize();
        String stats = system.generateStatistics();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        assertTrue(stats.length() > 20);
    }


    @Test
    void roleManagerGuardPreventsRemoveWhenAssigned() {
        system.initialize();
        var admin = system.getUserManager().findByUsername("admin").orElseThrow();
        var adminRole = system.getRoleManager().findByName("Admin").orElseThrow();

        assertTrue(system.getAssignmentManager().userHasRole(admin, adminRole));

        assertThrows(IllegalStateException.class, () ->
                system.getRoleManager().remove(adminRole));
    }

    @Test
    void allManagersAccessible() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());
    }
}