import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Main {
    public static void main(String[] args) {
        System.out.println("1.1 Users");
        User u1 = User.create("username", "Full Name", "email@gmail.com");
        User u2 = User.create("operator", "Operator Name", "operator@company.com");
        System.out.println("u1: " + u1.format());
        System.out.println("u2: " + u2.format());

        System.out.println("\n1.2 Permissions and roles");
        Permission read = new Permission("READ", "users", "Read users");
        Permission write = new Permission("WRITE", "reports", "Write reports");
        Permission delete = new Permission("DELETE", "settings", "Delete settings");

        Role adminRole = new Role("admin", "Admin permissions");
        adminRole.addPermission(read);
        adminRole.addPermission(write);
        adminRole.addPermission(delete);
        System.out.println("adminRole: " + adminRole.format());

        System.out.println("\n1.3 Assignment metadata");
        AssignmentMetadata meta = AssignmentMetadata.now(u2.username(), "Role assignment");
        System.out.println("meta: " + meta.format());

        System.out.println("\n1.4 Permanent assignment");
        PermanentAssignment pa = new PermanentAssignment(u1, adminRole, meta);
        System.out.println(pa.summary());
        pa.revoke();
        System.out.println("After revoke: revoked=" + pa.isRevoked() + ", active=" + pa.isActive());

        System.out.println("\n1.5 Temporary assignment");
        TemporaryAssignment ta = new TemporaryAssignment(u1, adminRole, meta,
                LocalDate.parse("2030-01-01").atStartOfDay().toString());
        System.out.println(ta.summary());
        ta.extend("2035-09-20");
        System.out.println("After extend(2035): " + ta.summary());
        ta.extend("2005-09-20");
        System.out.println("After extend(2005): " + ta.summary());

        demoFiltersAndSorters();
    }

    static void demoFiltersAndSorters() {
        System.out.println("\n2.1 User filters");
        var users = List.of(
                User.create("username", "User One", "user@company.com"),
                User.create("operator", "Operator", "operator@company.com"),
                User.create("tester", "Test User", "test@gmail.com"),
                User.create("john_admin", "John Admin", "john@company.com")
        );

        var companyFilter = UserFilters.byEmailDomain("company.com");
        users.stream()
                .filter(companyFilter::test)
                .map(User::format)
                .forEach(System.out::println);

        System.out.println("\n2.2 Role filters");
        var perm = new Permission("READ", "users", "Test");
        var adminRole = new Role("ADMIN", "");
        adminRole.addPermission(perm);
        var userRole = new Role("USER", "");
        var roles = List.of(adminRole, userRole);

        var powerfulFilter = RoleFilters.hasAtLeastNPermissions(1);
        roles.stream()
                .filter(powerfulFilter::test)
                .map(Role::getName)
                .forEach(System.out::println);

        System.out.println("\n2.3 Assignment filters");
        var meta = AssignmentMetadata.now("admin", "test");
        var assignments = List.<RoleAssignment>of(
                new PermanentAssignment(users.get(0), adminRole, meta),
                new PermanentAssignment(users.get(3), adminRole, meta)
        );

        var activeFilter = AssignmentFilters.activeOnly();
        assignments.stream()
                .filter(activeFilter::test)
                .map(RoleAssignment::assignmentId)
                .forEach(System.out::println);

        System.out.println("\n2.4 Sorters");
        System.out.println("Users by username:");
        users.stream()
                .sorted(UserSorters.byUsername())
                .map(User::username)
                .forEach(System.out::println);

        System.out.println("Roles by permission count:");
        roles.stream()
                .sorted(RoleSorters.byPermissionCount())
                .map(Role::getName)
                .forEach(System.out::println);
    }
}
