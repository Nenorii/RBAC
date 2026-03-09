import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    @Test
    void generateUserReport_noUsers() {
        UserManager um = new UserManager();
        AssignmentManager am = new AssignmentManager(um, new RoleManager());
        ReportGenerator rg = new ReportGenerator();

        String report = rg.generateUserReport(um, am);

        assertTrue(report.contains("User Report"));
        assertTrue(report.contains("No users."));
    }

    @Test
    void generateUserReport_withUsersAndRoles() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        ReportGenerator rg = new ReportGenerator();

        User u1 = User.create("alice", "Alice A", "alice@example.com");
        User u2 = User.create("bob", "Bob B", "bob@example.com");
        um.add(u1);
        um.add(u2);

        Role r1 = new Role("Admin", "Админ");
        Role r2 = new Role("Viewer", "Только чтение");
        rm.add(r1);
        rm.add(r2);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "test");
        PermanentAssignment a1 = new PermanentAssignment(u1, r1, meta);
        PermanentAssignment a2 = new PermanentAssignment(u2, r2, meta);
        am.add(a1);
        am.add(a2);

        String report = rg.generateUserReport(um, am);

        assertTrue(report.contains("User: alice"));
        assertTrue(report.contains("User: bob"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Viewer"));
    }

    @Test
    void generateRoleReport_noRoles() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        ReportGenerator rg = new ReportGenerator();

        String report = rg.generateRoleReport(rm, am);

        assertTrue(report.contains("Role Report"));
        assertTrue(report.contains("No roles."));
    }

    @Test
    void generateRoleReport_countsUsersPerRole() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        ReportGenerator rg = new ReportGenerator();

        User u1 = User.create("alice", "Alice A", "alice@example.com");
        User u2 = User.create("bob", "Bob B", "bob@example.com");
        um.add(u1);
        um.add(u2);

        Role r1 = new Role("Admin", "Админ");
        rm.add(r1);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "test");
        am.add(new PermanentAssignment(u1, r1, meta));
        am.add(new PermanentAssignment(u2, r1, meta));

        String report = rg.generateRoleReport(rm, am);

        assertTrue(report.contains("Role: Admin"));
        assertTrue(report.contains("Users: 2"));
    }

    @Test
    void generatePermissionMatrix_noUsers() {
        UserManager um = new UserManager();
        AssignmentManager am = new AssignmentManager(um, new RoleManager());
        ReportGenerator rg = new ReportGenerator();

        String report = rg.generatePermissionMatrix(um, am);

        assertTrue(report.contains("Permission Matrix (Users x Resources)"));
        assertTrue(report.contains("No users."));
    }

    @Test
    void generatePermissionMatrix_simpleCase() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        ReportGenerator rg = new ReportGenerator();

        User u1 = User.create("alice", "Alice A", "alice@example.com");
        User u2 = User.create("bob", "Bob B", "bob@example.com");
        um.add(u1);
        um.add(u2);

        Role r1 = new Role("Admin", "Админ");
        rm.add(r1);

        Permission pUsersRead = new Permission("USER_READ", "users", "Чтение пользователей");
        Permission pRolesRead = new Permission("ROLE_READ", "roles", "Чтение ролей");
        r1.addPermission(pUsersRead);
        r1.addPermission(pRolesRead);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "test");
        am.add(new PermanentAssignment(u1, r1, meta));

        String report = rg.generatePermissionMatrix(um, am);

        assertTrue(report.contains("User"));
        assertTrue(report.contains("users"));
        assertTrue(report.contains("roles"));

        assertTrue(report.contains("alice"));

        assertTrue(report.contains("bob"));
    }
}
