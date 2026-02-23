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
}
