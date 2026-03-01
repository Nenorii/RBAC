import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentManagerTest {

    UserManager userManager;
    RoleManager roleManager;
    AssignmentManager assignmentManager;
    User user;
    Role role;
    AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
        user = User.create("john", "John Doe", "john@mail.com");
        role = new Role("Admin", "");
        meta = AssignmentMetadata.now("admin", "test");
        userManager.add(user);
        roleManager.add(role);
    }

    @Test
    void addAndFindByUser() {
        var pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);
        var list = assignmentManager.findByUser(user);
        assertEquals(1, list.size());
        assertEquals(pa, list.get(0));
    }

    @Test
    void addWithoutUserThrows() {
        var u = User.create("ghost", "Ghost", "ghost@m.com");
        var pa = new PermanentAssignment(u, role, meta);
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(pa));
    }

    @Test
    void addDuplicateUserRoleThrows() {
        assignmentManager.add(new PermanentAssignment(user, role, meta));
        assertThrows(IllegalStateException.class, () -> assignmentManager.add(new PermanentAssignment(user, role, meta)));
    }

    @Test
    void userHasRole() {
        assignmentManager.add(new PermanentAssignment(user, role, meta));
        assertTrue(assignmentManager.userHasRole(user, role));
    }

    @Test
    void userHasPermission() {
        var perm = new Permission("read", "users", "x");
        role.addPermission(perm);
        assignmentManager.add(new PermanentAssignment(user, role, meta));
        assertTrue(assignmentManager.userHasPermission(user, "read", "users"));
    }

    @Test
    void getUserPermissions() {
        var perm = new Permission("write", "roles", "x");
        role.addPermission(perm);
        assignmentManager.add(new PermanentAssignment(user, role, meta));
        var perms = assignmentManager.getUserPermissions(user);
        assertTrue(perms.stream().anyMatch(p -> p.name().equals("WRITE") && p.resource().equals("roles")));
    }

    @Test
    void revokeAssignment() {
        var pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);
        assertTrue(pa.isActive());
        assignmentManager.revokeAssignment(pa.assignmentId());
        assertFalse(pa.isActive());
    }

    @Test
    void extendTemporaryAssignment() {
        var ta = new TemporaryAssignment(user, role, meta, "2025-01-01");
        assignmentManager.add(ta);
        assignmentManager.extendTemporaryAssignment(ta.assignmentId(), "2030-12-31");
        assertEquals("2030-12-31", ta.getExpiresAt());
    }

    @Test
    void getActiveAssignments() {
        var pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);
        pa.revoke();
        var active = assignmentManager.getActiveAssignments();
        assertEquals(0, active.size());
    }
}