import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 50;
    private static final Random random = new Random();

    private static final AtomicInteger usersCreated = new AtomicInteger(0);
    private static final AtomicInteger rolesCreated = new AtomicInteger(0);
    private static final AtomicInteger assignmentsCreated = new AtomicInteger(0);
    private static final AtomicInteger errors = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("    НАГРУЗОЧНЫЙ ТЕСТ RBAC СИСТЕМЫ");
        System.out.println("Потоков: " + THREAD_COUNT);
        System.out.println("Операций на поток: " + OPERATIONS_PER_THREAD);
        System.out.println("Всего операций: " + (THREAD_COUNT * OPERATIONS_PER_THREAD));

        RBACSystem system = new RBACSystem();
        system.initialize();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<TestResult>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> runTest(system, threadId)));
        }

        int totalOps = 0;
        int totalSuccess = 0;
        for (Future<TestResult> future : futures) {
            try {
                TestResult result = future.get(60, TimeUnit.SECONDS);
                totalOps += result.operations;
                totalSuccess += result.success;
            } catch (TimeoutException e) {
                System.err.println("Поток не завершился вовремя");
                errors.incrementAndGet();
            } catch (Exception e) {
                System.err.println("Ошибка при получении результата: " + e.getMessage());
                errors.incrementAndGet();
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        system.shutdown();

        System.out.println("\n");
        System.out.println("         РЕЗУЛЬТАТЫ ТЕСТА");
        System.out.println("Время выполнения: " + duration + " ms");
        System.out.println("Всего операций: " + totalOps);
        System.out.println("Успешных операций: " + totalSuccess);
        System.out.println("Ошибок: " + errors.get());
        System.out.println("Успешность: " + String.format("%.2f%%", (double) totalSuccess / totalOps * 100));
        System.out.println("\n      ИТОГОВАЯ СТАТИСТИКА СИСТЕМЫ");
        System.out.println(system.generateStatistics());
        System.out.println("\n      СОЗДАНО В ХОДЕ ТЕСТА");
        System.out.println("Пользователей создано: " + usersCreated.get());
        System.out.println("Ролей создано: " + rolesCreated.get());
        System.out.println("Назначений создано: " + assignmentsCreated.get());

        verifySystemState(system);
    }

    private static TestResult runTest(RBACSystem system, int threadId) {
        int success = 0;
        int operations = 0;

        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
            try {
                int operation = random.nextInt(8);
                switch (operation) {
                    case 0 -> createUser(system, threadId, i);
                    case 1 -> createRole(system, threadId, i);
                    case 2 -> assignRole(system, threadId);
                    case 3 -> listUsers(system);
                    case 4 -> listRoles(system);
                    case 5 -> searchUsers(system);
                    case 6 -> searchRoles(system);
                    case 7 -> generateReport(system);
                }
                success++;
            } catch (Exception e) {
                errors.incrementAndGet();
                if (errors.get() % 50 == 0) {
                    System.err.println("[Поток " + threadId + "] Ошибка: " + e.getMessage());
                }
            }
            operations++;
        }

        return new TestResult(operations, success);
    }

    private static void createUser(RBACSystem system, int threadId, int iteration) {
        String username = "user" + threadId + iteration + System.currentTimeMillis();
        String fullName = "Test User " + threadId + "_" + iteration;
        String email = username + "@test.com";

        try {
            User user = User.create(username, fullName, email);
            system.getUserManager().add(user);
            usersCreated.incrementAndGet();
            if (usersCreated.get() % 50 == 0) {
                System.out.println("[СОЗДАН ПОЛЬЗОВАТЕЛЬ] " + username);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[ОШИБКА СОЗДАНИЯ] " + username + ": " + e.getMessage());
        }
    }

    private static void createRole(RBACSystem system, int threadId, int iteration) {
        String roleName = "test_role_" + threadId + "_" + iteration + "_" + System.currentTimeMillis();
        String description = "Test role " + threadId + "_" + iteration;

        try {
            Role role = new Role(roleName, description);
            Permission p = new Permission("TEST_PERM_" + threadId, "test_resource", "Test permission");
            role.addPermission(p);
            system.getRoleManager().add(role);
            rolesCreated.incrementAndGet();

            if (rolesCreated.get() % 100 == 0) {
                System.out.println("[СОЗДАНА РОЛЬ] " + roleName);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private static void assignRole(RBACSystem system, int threadId) {
        var users = system.getUserManager().findAll();
        var roles = system.getRoleManager().findAll();

        if (users.isEmpty() || roles.isEmpty()) {
            return;
        }

        User randomUser = users.get(random.nextInt(users.size()));
        Role randomRole = roles.get(random.nextInt(roles.size()));

        try {
            if (!system.getAssignmentManager().userHasRole(randomUser, randomRole)) {
                AssignmentMetadata meta = AssignmentMetadata.now("load_test", "Нагрузочный тест");

                RoleAssignment assignment;
                if (random.nextBoolean()) {
                    assignment = new PermanentAssignment(randomUser, randomRole, meta);
                } else {
                    int days = random.nextInt(30) + 1;
                    String expiryDate = DateUtils.extend(DateUtils.getCurrentDateTime(), days);
                    assignment = new TemporaryAssignment(randomUser, randomRole, meta, expiryDate);
                }
                system.getAssignmentManager().add(assignment);
                assignmentsCreated.incrementAndGet();

                if (assignmentsCreated.get() % 100 == 0) {
                    System.out.println("[НАЗНАЧЕНИЕ] " + randomUser.username() + " -> " + randomRole.getName());
                }
            }
        } catch (Exception e) {
        }
    }

    private static void listUsers(RBACSystem system) {
        system.getUserManager().findAll();
    }

    private static void listRoles(RBACSystem system) {
        system.getRoleManager().findAll();
    }

    private static void searchUsers(RBACSystem system) {
        system.getUserManager().findByFilter(user -> user.email().contains("test"));
    }

    private static void searchRoles(RBACSystem system) {
        system.getRoleManager().findByFilter(role -> role.getName().contains("test"));
    }

    private static void generateReport(RBACSystem system) {
        system.getReportGenerator().generateUserReport(
                system.getUserManager(),
                system.getAssignmentManager()
        );
    }

    private static void verifySystemState(RBACSystem system) {
        System.out.println("\n      ПРОВЕРКА СОСТОЯНИЯ СИСТЕМЫ");

        var users = system.getUserManager().findAll();
        var roles = system.getRoleManager().findAll();
        var assignments = system.getAssignmentManager().findAll();

        long uniqueUsernames = users.stream().map(User::username).distinct().count();
        if (uniqueUsernames != users.size()) {
            System.err.println("ОБНАРУЖЕНЫ ДУБЛИКАТЫ ПОЛЬЗОВАТЕЛЕЙ! Уникальных: " + uniqueUsernames + ", всего: " + users.size());
        } else {
            System.out.println("Дубликатов пользователей нет");
        }

        long uniqueRoleNames = roles.stream().map(Role::getName).distinct().count();
        if (uniqueRoleNames != roles.size()) {
            System.err.println("ОБНАРУЖЕНЫ ДУБЛИКАТЫ РОЛЕЙ! Уникальных: " + uniqueRoleNames + ", всего: " + roles.size());
        } else {
            System.out.println("Дубликатов ролей нет");
        }

        boolean assignmentsValid = true;
        for (RoleAssignment a : assignments) {
            if (!system.getUserManager().exists(a.user().username())) {
                System.err.println("Назначение ссылается на несуществующего пользователя: " + a.user().username());
                assignmentsValid = false;
            }
            if (!system.getRoleManager().exists(a.role().getName())) {
                System.err.println("Назначение ссылается на несуществующую роль: " + a.role().getName());
                assignmentsValid = false;
            }
        }
        if (assignmentsValid) {
            System.out.println("Все назначения ссылаются на существующих пользователей и роли");
        }

        long active = assignments.stream().filter(RoleAssignment::isActive).count();
        long inactive = assignments.size() - active;
        System.out.println("Назначений: всего=" + assignments.size() + ", активных=" + active + ", неактивных=" + inactive);

        System.out.println("        ПРОВЕРКА ЗАВЕРШЕНА");
    }

    private record TestResult(int operations, int success) {}
}