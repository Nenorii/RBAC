import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final ReportGenerator reportGenerator;
    private final BackgroundExecutor backgroundExecutor;
    private final ExpiredAssignmentsChecker expiredChecker;
    private String currentUser;

    RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.auditLog = new AuditLog();
        this.reportGenerator = new ReportGenerator();
        this.backgroundExecutor = new BackgroundExecutor();
        this.expiredChecker = new ExpiredAssignmentsChecker(assignmentManager, auditLog);
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

    BackgroundExecutor getBackgroundExecutor() {
        return backgroundExecutor;
    }

    ExpiredAssignmentsChecker getExpiredChecker() {
        return expiredChecker;
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

        startScheduledTasks();
    }

    private void startScheduledTasks() {
        backgroundExecutor.scheduleAtFixedRate(() -> {
            try {
                int expired = expiredChecker.checkAndMarkExpired();
                if (expired > 0) {
                    auditLog.log("SCHEDULER_EXPIRED_CHECK", "scheduler", "system",
                            "Проверка истекших: найдено=" + expired);
                }
            } catch (Exception e) {
                System.err.println("[SCHEDULER] Ошибка при проверке истекших: " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);

        backgroundExecutor.scheduleAtFixedRate(() -> {
            try {
                String stats = expiredChecker.generateStatisticsReport();
                System.out.println("[SCHEDULER] " + stats);
                auditLog.log("SCHEDULER_STATS", "scheduler", "system", stats);
            } catch (Exception e) {
                System.err.println("[SCHEDULER] Ошибка при генерации статистики: " + e.getMessage());
            }
        }, 15, 30, TimeUnit.SECONDS);
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
        sb.append("Expired processed by scheduler: ").append(expiredChecker.getTotalExpiredProcessed()).append("\n");

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

    public CompletableFuture<Void> saveDataAsync(String filename) {
        return backgroundExecutor.submit(() -> {
            auditLog.saveToFile(filename);
            System.out.println("[ASYNC] Данные сохранены в " + filename + " (поток: " + Thread.currentThread().getName() + ")");
        });
    }

    public CompletableFuture<String> generateUserReportAsync() {
        return backgroundExecutor.submit(() -> {
            String report = reportGenerator.generateUserReport(userManager, assignmentManager);
            System.out.println("[ASYNC] Отчёт по пользователям сгенерирован (поток: " + Thread.currentThread().getName() + ")");
            return report;
        });
    }

    public CompletableFuture<String> generatePermissionMatrixAsync() {
        return backgroundExecutor.submit(() -> {
            String matrix = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
            System.out.println("[ASYNC] Матрица прав сгенерирована (поток: " + Thread.currentThread().getName() + ")");
            return matrix;
        });
    }

    public CompletableFuture<String> generateRoleReportAsync() {
        return backgroundExecutor.submit(() -> {
            String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);
            System.out.println("[ASYNC] Отчёт по ролям сгенерирован (поток: " + Thread.currentThread().getName() + ")");
            return report;
        });
    }

    public void shutdown() {
        backgroundExecutor.shutdown();
        auditLog.shutdown();
    }
}