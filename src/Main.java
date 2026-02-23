import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

void main() {
        User user1 = new User("username", "full name", "email@gmail.com");
        User user2 = new User("operator", "full name", "email@gmail.com");

        Role role = new Role("admin", "admin permissions");

        Permission read = new Permission("READ", "users", "Can read users");
        Permission write = new Permission("WRITE", "reports", "Can write reports");
        Permission delete = new Permission("DELETE", "settings", "Can delete users");

        role.addPermission(read);
        role.addPermission(write);
        role.addPermission(delete);

        AssignmentMetadata am = AssignmentMetadata.now(user2.username(), "with some reason");

        PermanentAssignment pa = new PermanentAssignment(user1, role, am);
        System.out.println(pa.summary());

        pa.revoke();
        System.out.println(pa.summary());

        TemporaryAssignment ta = new TemporaryAssignment(user1, role, am);

        ta.extend(LocalDate.parse("2035-09-20").atStartOfDay().toString());
        System.out.println(ta.summary());

        ta.extend(LocalDate.parse("2005-09-20").atStartOfDay().toString());
        System.out.println(ta.summary());

        User user3 = new User("testuser", "Test User", "test@company.com");
        User user4 = new User("john_admin", "John Doe", "john@company.com");

        Role userRole = new Role("USER", "basic permissions");
        Role adminRole2 = new Role("ADMIN", "admin permissions");

        AssignmentMetadata am2 = AssignmentMetadata.now("operator", "test reason");
        PermanentAssignment pa2 = new PermanentAssignment(user3, userRole, am2);
        PermanentAssignment pa3 = new PermanentAssignment(user4, adminRole2, am2);

        List<User> users = Arrays.asList(user1, user2, user3, user4);
        List<Role> roles = Arrays.asList(role, userRole, adminRole2);
        List<RoleAssignment> assignments = Arrays.asList(pa, pa2, pa3);

        System.out.println("1. User Filters:");
        UserFilter byCompany = UserFilters.byEmailDomain("company.com");
        List<User> companyUsers = users.stream()
                .filter(byCompany::test)
                .collect(Collectors.toList());
        System.out.println("Company users: " + companyUsers.stream().map(User::username).toList());

        System.out.println("\n2. Role Filters:");
        RoleFilter manyPermissions = RoleFilters.hasAtLeastNPermissions(2);
        List<Role> powerfulRoles = roles.stream()
                .filter(manyPermissions::test)
                .collect(Collectors.toList());
        System.out.println("Roles with 2+ permissions: " + powerfulRoles.stream().map(Role::getName).toList());

        System.out.println("\n3. Assignment Filters:");
        AssignmentFilter activeUser1 = AssignmentFilters.byUser(user1)
                .and(AssignmentFilters.activeOnly());
        List<RoleAssignment> user1Active = assignments.stream()
                .filter(activeUser1::test)
                .collect(Collectors.toList());
        System.out.println("User1 active assignments: " + user1Active.size());

        System.out.println("\n4. Sorting:");
        System.out.println("Users by username: " +
                users.stream()
                        .sorted(UserSorters.byUsername())
                        .map(User::username)
                        .toList());

        System.out.println("Roles by permission count: " +
                roles.stream()
                        .sorted(RoleSorters.byPermissionCount())
                        .map(Role::getName)
                        .toList());

        System.out.println("\n5. Complex combinations:");
        UserFilter complexUser = UserFilters.byUsernameContains("admin")
                .or(UserFilters.byEmail("test@company.com"));

        RoleFilter complexRole = RoleFilters.byNameContains("ADMIN")
                .or(RoleFilters.hasPermission(write));

        List<User> complexUsers = users.stream()
                .filter(complexUser::test)
                .sorted(UserSorters.byFullName())
                .collect(Collectors.toList());

        System.out.println("Complex user filter + sort: " +
                complexUsers.stream().map(User::username).toList());

}
