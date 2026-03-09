final class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final ReportGenerator reportGenerator;
    private String currentUser;

    RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.auditLog = new AuditLog();
        this.reportGenerator = new ReportGenerator();
        roleManager.setRemoveGuard(role ->
                !assignmentManager.findByRole(role).isEmpty()
        );
    }

    UserManager getUserManager() {
        return userManager;
    }

    RoleManager getRoleManager() {
        return roleManager;
    }

    AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    AuditLog getAuditLog() {
        return auditLog;
    }

    ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    void setCurrentUser(String username) {
        this.currentUser = username;
    }

    String getCurrentUser() {
        return currentUser;
    }

    void initialize() {
        Permission userRead    = new Permission("USER_READ", "users", "Чтение пользователей");
        Permission userWrite   = new Permission("USER_WRITE", "users", "Изменение пользователей");
        Permission userDelete  = new Permission("USER_DELETE", "users", "Удаление пользователей");

        Permission roleRead    = new Permission("ROLE_READ", "roles", "Чтение ролей");
        Permission roleWrite   = new Permission("ROLE_WRITE", "roles", "Изменение ролей");
        Permission roleDelete  = new Permission("ROLE_DELETE", "roles", "Удаление ролей");

        Permission assignRead  = new Permission("ASSIGN_READ", "assignments", "Чтение назначений");
        Permission assignWrite = new Permission("ASSIGN_WRITE", "assignments", "Изменение назначений");

        Role admin = new Role("Admin", "Системный администратор");
        admin.addPermission(userRead);
        admin.addPermission(userWrite);
        admin.addPermission(userDelete);
        admin.addPermission(roleRead);
        admin.addPermission(roleWrite);
        admin.addPermission(roleDelete);
        admin.addPermission(assignRead);
        admin.addPermission(assignWrite);

        Role manager = new Role("Manager", "Менеджер с ограниченными правами");
        manager.addPermission(userRead);
        manager.addPermission(roleRead);
        manager.addPermission(assignRead);
        manager.addPermission(assignWrite);

        Role viewer = new Role("Viewer", "Только чтение");
        viewer.addPermission(userRead);
        viewer.addPermission(roleRead);
        viewer.addPermission(assignRead);

        roleManager.add(admin);
        roleManager.add(manager);
        roleManager.add(viewer);

        User adminUser = User.create("admin", "Test Administrator", "admin@example.com");
        userManager.add(adminUser);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Инициализация системы");
        PermanentAssignment pa = new PermanentAssignment(adminUser, admin, meta);
        assignmentManager.add(pa);

        setCurrentUser("admin");

        auditLog.log("SYSTEM_INIT", "system", "-", "Инициализация RBACSystem");
    }

    String generateStatistics() {
        int usersCount = userManager.count();
        int rolesCount = roleManager.count();
        int totalAssignments = assignmentManager.count();
        int activeAssignments = assignmentManager.getActiveAssignments().size();
        int expiredAssignments = assignmentManager.getExpiredAssignments().size();

        double avgRolesPerUser = usersCount == 0
                ? 0.0
                : (double) totalAssignments / usersCount;

        StringBuilder sb = new StringBuilder();
        sb.append("RBAC Statistics\n");
        sb.append("Users: ").append(usersCount).append("\n");
        sb.append("Roles: ").append(rolesCount).append("\n");
        sb.append("Assignments: ").append(totalAssignments)
                .append(" (active=").append(activeAssignments)
                .append(", expired=").append(expiredAssignments).append(")\n");
        sb.append(String.format("Average roles per user: %.2f%n", avgRolesPerUser));

        var roleToCount = new java.util.HashMap<Role, Integer>();
        for (RoleAssignment a : assignmentManager.findAll()) {
            roleToCount.merge(a.role(), 1, Integer::sum);
        }
        var topRoles = roleToCount.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .toList();

        sb.append("Top roles:\n");
        if (topRoles.isEmpty()) {
            sb.append("  (no assignments yet)\n");
        } else {
            for (var e : topRoles) {
                sb.append("  ")
                        .append(e.getKey().getName())
                        .append(" - ")
                        .append(e.getValue())
                        .append(" assignments\n");
            }
        }

        return sb.toString();
    }
}
