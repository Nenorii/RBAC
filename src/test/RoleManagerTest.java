import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {

    RoleManager manager;
    Permission perm;

    @BeforeEach
    void setUp() {
        manager = new RoleManager();
        perm = new Permission("read", "users", "Просмотр");
    }

    @Test
    void addAndFindById() {
        var r = new Role("Admin", "Описание");
        manager.add(r);
        assertEquals(r, manager.findById(r.getId()).orElse(null));
    }

    @Test
    void findByName() {
        var r = new Role("Viewer", "Просмотр");
        manager.add(r);
        assertEquals(r, manager.findByName("Viewer").orElse(null));
    }

    @Test
    void addDuplicateNameThrows() {
        manager.add(new Role("Admin", ""));
        assertThrows(IllegalArgumentException.class, () -> manager.add(new Role("Admin", "другое")));
    }

    @Test
    void removeWithGuard() {
        var r = new Role("Admin", "");
        manager.add(r);
        manager.setRemoveGuard(role -> true); // всегда "в использовании"
        assertThrows(IllegalStateException.class, () -> manager.remove(r));
    }

    @Test
    void addPermissionToRole() {
        var r = new Role("Admin", "");
        manager.add(r);
        manager.addPermissionToRole("Admin", perm);
        assertTrue(r.hasPermission(perm));
    }

    @Test
    void removePermissionFromRole() {
        var r = new Role("Admin", "");
        r.addPermission(perm);
        manager.add(r);
        manager.removePermissionFromRole("Admin", perm);
        assertFalse(r.hasPermission(perm));
    }

    @Test
    void findRolesWithPermission() {
        var r1 = new Role("R1", "");
        r1.addPermission(perm);
        manager.add(r1);
        manager.add(new Role("R2", ""));
        var list = manager.findRolesWithPermission("read", "users");
        assertEquals(1, list.size());
        assertEquals("R1", list.get(0).getName());
    }

    @Test
    void findByFilter() {
        var r = new Role("Administrator", "");
        r.addPermission(perm);
        manager.add(r);
        var list = manager.findByFilter(RoleFilters.byNameContains("Admin"));
        assertEquals(1, list.size());
    }
}