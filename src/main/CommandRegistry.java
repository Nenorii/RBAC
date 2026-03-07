import java.util.List;
import java.util.Scanner;
import java.util.Comparator;

class CommandRegistry {

    static void registerAllCommands(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerPermissionCommands(parser);
        registerServiceCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "Список всех пользователей", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();
            if (users.isEmpty()) {
                System.out.println("Пользователей нет.");
                return;
            }
            System.out.println("\n=== Список пользователей ===");
            System.out.printf("%-20s %-30s %-30s%n", "Username", "Полное имя", "Email");
            System.out.println("-".repeat(80));
            for (User u : users) {
                System.out.printf("%-20s %-30s %-30s%n", u.username(), u.fullName(), u.email());
            }
            System.out.println();
        });

        parser.registerCommand("user-create", "Создать нового пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Полное имя: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            try {
                User user = User.create(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("Пользователь создан: " + user.format());
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "Просмотр информации о пользователе", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            System.out.println("\n" + user.get().format());
            var assignments = system.getAssignmentManager().findByUser(user.get());
            if (assignments.isEmpty()) {
                System.out.println("Назначенных ролей нет.");
            } else {
                System.out.println("Роли:");
                for (var a : assignments) {
                    System.out.println("  - " + a.role().getName() + " [" + a.assignmentType() + ", " + (a.isActive() ? "ACTIVE" : "INACTIVE") + "]");
                }
            }
            var permissions = system.getAssignmentManager().getUserPermissions(user.get());
            System.out.println("Все права (" + permissions.size() + "):");
            if (permissions.isEmpty()) {
                System.out.println("  —");
            } else {
                for (Permission p : permissions) {
                    System.out.println("  - " + p.format());
                }
            }
            System.out.println();
        });

        parser.registerCommand("user-update", "Обновить данные пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Новое полное имя: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Новый email: ");
            String email = scanner.nextLine().trim();
            try {
                system.getUserManager().update(username, fullName, email);
                System.out.println("Данные обновлены.");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Удалить пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            System.out.print("Подтвердите удаление (введите 'да'): ");
            String confirm = scanner.nextLine().trim();
            if (!"да".equalsIgnoreCase(confirm)) {
                System.out.println("Отменено.");
                return;
            }
            var assignments = system.getAssignmentManager().findByUser(user.get());
            for (var a : assignments) {
                system.getAssignmentManager().remove(a);
            }
            system.getUserManager().remove(user.get());
            System.out.println("Пользователь удалён.");
        });

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам", (scanner, system) -> {
            System.out.println("Выберите фильтр:");
            System.out.println("1. По username (содержит)");
            System.out.println("2. По email (содержит)");
            System.out.println("3. По домену email");
            System.out.println("4. По полному имени (содержит)");
            System.out.print("Выбор: ");
            String choice = scanner.nextLine().trim();
            UserFilter filter;
            switch (choice) {
                case "1" -> {
                    System.out.print("Подстрока username: ");
                    String sub = scanner.nextLine().trim();
                    filter = UserFilters.byUsernameContains(sub);
                }
                case "2" -> {
                    System.out.print("Подстрока email: ");
                    String sub = scanner.nextLine().trim();
                    filter = user -> user.email().toLowerCase().contains(sub.toLowerCase());
                }
                case "3" -> {
                    System.out.print("Домен (например, @example.com): ");
                    String domain = scanner.nextLine().trim();
                    filter = UserFilters.byEmailDomain(domain);
                }
                case "4" -> {
                    System.out.print("Подстрока полного имени: ");
                    String sub = scanner.nextLine().trim();
                    filter = UserFilters.byFullNameContains(sub);
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }
            var results = system.getUserManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size());
                for (User u : results) {
                    System.out.println("  " + u.format());
                }
            }
        });
    }

    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "Список всех ролей", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("Ролей нет.");
                return;
            }
            System.out.println("\n=== Список ролей ===");
            System.out.printf("%-20s %-10s %-40s%n", "Название", "Прав", "ID");
            System.out.println("-".repeat(70));
            for (Role r : roles) {
                System.out.printf("%-20s %-10d %-40s%n", r.getName(), r.getPermissions().size(), r.getId());
            }
            System.out.println();
        });

        parser.registerCommand("role-create", "Создать новую роль", (scanner, system) -> {
            System.out.print("Название роли: ");
            String name = scanner.nextLine().trim();
            System.out.print("Описание: ");
            String desc = scanner.nextLine().trim();
            try {
                Role role = new Role(name, desc);
                system.getRoleManager().add(role);
                System.out.println("Роль создана: " + role.getName() + " [" + role.getId() + "]");
                System.out.print("Добавить права? (да/нет): ");
                if ("да".equalsIgnoreCase(scanner.nextLine().trim())) {
                    while (true) {
                        System.out.print("Имя права (или пусто для завершения): ");
                        String pName = scanner.nextLine().trim();
                        if (pName.isBlank()) break;
                        System.out.print("Ресурс: ");
                        String resource = scanner.nextLine().trim();
                        System.out.print("Описание права: ");
                        String pDesc = scanner.nextLine().trim();
                        Permission p = new Permission(pName, resource, pDesc);
                        role.addPermission(p);
                        System.out.println("Право добавлено.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "Просмотр роли", (scanner, system) -> {
            System.out.print("Имя роли: ");
            String name = scanner.nextLine().trim();
            var role = system.getRoleManager().findByName(name);
            if (role.isEmpty()) {
                System.out.println("Роль не найдена.");
                return;
            }
            System.out.println("\n" + role.get().format());
        });

        parser.registerCommand("role-delete", "Удалить роль", (scanner, system) -> {
            System.out.print("Имя роли: ");
            String name = scanner.nextLine().trim();
            var role = system.getRoleManager().findByName(name);
            if (role.isEmpty()) {
                System.out.println("Роль не найдена.");
                return;
            }
            var assignments = system.getAssignmentManager().findByRole(role.get());
            if (!assignments.isEmpty()) {
                System.out.println("Роль назначена пользователям:");
                for (var a : assignments) {
                    System.out.println("  - " + a.user().username());
                }
            }
            System.out.print("Подтвердите удаление (введите 'да'): ");
            if (!"да".equalsIgnoreCase(scanner.nextLine().trim())) {
                System.out.println("Отменено.");
                return;
            }
            try {
                system.getRoleManager().remove(role.get());
                System.out.println("Роль удалена.");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, system) -> {
            System.out.print("Имя роли: ");
            String roleName = scanner.nextLine().trim();
            System.out.print("Имя права: ");
            String pName = scanner.nextLine().trim();
            System.out.print("Ресурс: ");
            String resource = scanner.nextLine().trim();
            System.out.print("Описание: ");
            String desc = scanner.nextLine().trim();
            try {
                Permission p = new Permission(pName, resource, desc);
                system.getRoleManager().addPermissionToRole(roleName, p);
                System.out.println("Право добавлено к роли.");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, system) -> {
            System.out.print("Имя роли: ");
            String roleName = scanner.nextLine().trim();
            var role = system.getRoleManager().findByName(roleName);
            if (role.isEmpty()) {
                System.out.println("Роль не найдена.");
                return;
            }
            var perms = role.get().getPermissions().stream().toList();
            if (perms.isEmpty()) {
                System.out.println("У роли нет прав.");
                return;
            }
            System.out.println("Права роли:");
            for (int i = 0; i < perms.size(); i++) {
                System.out.println((i + 1) + ". " + perms.get(i).format());
            }
            System.out.print("Номер права для удаления: ");
            String raw = scanner.nextLine().trim();
            int idx;
            try {
                idx = Integer.parseInt(raw) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод числа.");
                return;
            }
            if (idx < 0 || idx >= perms.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            system.getRoleManager().removePermissionFromRole(roleName, perms.get(idx));
            System.out.println("Право удалено.");
        });

        parser.registerCommand("role-search", "Поиск ролей", (scanner, system) -> {
            System.out.println("Поиск ролей:");
            System.out.println("1. По имени (содержит)");
            System.out.println("2. По наличию конкретного права");
            System.out.println("3. По минимальному количеству прав");
            System.out.print("Выбор: ");
            String choice = scanner.nextLine().trim();
            RoleFilter filter;
            switch (choice) {
                case "1" -> {
                    System.out.print("Подстрока имени: ");
                    String sub = scanner.nextLine().trim();
                    filter = RoleFilters.byNameContains(sub);
                }
                case "2" -> {
                    System.out.print("Имя права: ");
                    String pName = scanner.nextLine().trim();
                    System.out.print("Ресурс: ");
                    String resource = scanner.nextLine().trim();
                    filter = RoleFilters.hasPermission(pName, resource);
                }
                case "3" -> {
                    System.out.print("Минимум прав: ");
                    String raw = scanner.nextLine().trim();
                    int n;
                    try {
                        n = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        System.out.println("Неверный ввод числа.");
                        return;
                    }
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }
            var results = system.getRoleManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size());
                for (Role r : results) {
                    System.out.println("  " + r.getName() + " (" + r.getPermissions().size() + " прав)");
                }
            }
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Назначить роль пользователю", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            var roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("Ролей нет.");
                return;
            }
            System.out.println("Доступные роли:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println((i + 1) + ". " + roles.get(i).getName());
            }
            System.out.print("Выберите роль (номер): ");
            String rawRole = scanner.nextLine().trim();
            int roleIdx;
            try {
                roleIdx = Integer.parseInt(rawRole) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод числа.");
                return;
            }
            if (roleIdx < 0 || roleIdx >= roles.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            Role role = roles.get(roleIdx);
            System.out.print("Тип назначения (1-постоянное, 2-временное): ");
            String type = scanner.nextLine().trim();
            System.out.print("Причина назначения: ");
            String reason = scanner.nextLine().trim();
            AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), reason);
            try {
                if ("1".equals(type)) {
                    PermanentAssignment pa = new PermanentAssignment(user.get(), role, meta);
                    system.getAssignmentManager().add(pa);
                    System.out.println("Роль назначена (постоянно).");
                } else if ("2".equals(type)) {
                    System.out.print("Дата окончания (yyyy-MM-dd HH:mm): ");
                    String expiresAt = scanner.nextLine().trim();
                    TemporaryAssignment ta = new TemporaryAssignment(user.get(), role, meta, expiresAt);
                    system.getAssignmentManager().add(ta);
                    System.out.println("Роль назначена (временно до " + expiresAt + ").");
                } else {
                    System.out.println("Неверный тип.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            var assignments = system.getAssignmentManager().findByUser(user.get()).stream()
                    .filter(RoleAssignment::isActive).toList();
            if (assignments.isEmpty()) {
                System.out.println("Активных назначений нет.");
                return;
            }
            System.out.println("Активные назначения:");
            for (int i = 0; i < assignments.size(); i++) {
                var a = assignments.get(i);
                System.out.println((i + 1) + ". " + a.role().getName() + " [" + a.assignmentType() + "]");
            }
            System.out.print("Выберите назначение для отзыва (номер): ");
            String rawIdx = scanner.nextLine().trim();
            int idx;
            try {
                idx = Integer.parseInt(rawIdx) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод числа.");
                return;
            }
            if (idx < 0 || idx >= assignments.size()) {
                System.out.println("Неверный номер.");
                return;
            }
            try {
                system.getAssignmentManager().revokeAssignment(assignments.get(idx).assignmentId());
                System.out.println("Назначение отозвано.");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-list", "Список всех назначений", (scanner, system) -> {
            var all = system.getAssignmentManager().findAll();
            if (all.isEmpty()) {
                System.out.println("Назначений нет.");
                return;
            }
            System.out.println("\n=== Список назначений ===");
            System.out.printf("%-20s %-20s %-12s %-10s %-30s%n", "Username", "Роль", "Тип", "Статус", "Назначено");
            System.out.println("-".repeat(95));
            for (RoleAssignment a : all) {
                System.out.printf("%-20s %-20s %-12s %-10s %-30s%n",
                        a.user().username(), a.role().getName(), a.assignmentType(),
                        a.isActive() ? "ACTIVE" : "INACTIVE", a.metadata().assignedAt());
            }
            System.out.println();
        });

        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            var assignments = system.getAssignmentManager().findByUser(user.get());
            if (assignments.isEmpty()) {
                System.out.println("Назначений нет.");
            } else {
                System.out.println("Назначения для " + username + ":");
                for (RoleAssignment a : assignments) {
                    if (a instanceof AbstractRoleAssignment ara) {
                        System.out.println(ara.summary());
                        System.out.println();
                    } else {
                        System.out.println(a.toString());
                        System.out.println();
                    }
                }
            }
        });

        parser.registerCommand("assignment-list-role", "Пользователи с конкретной ролью", (scanner, system) -> {
            System.out.print("Имя роли: ");
            String roleName = scanner.nextLine().trim();
            var role = system.getRoleManager().findByName(roleName);
            if (role.isEmpty()) {
                System.out.println("Роль не найдена.");
                return;
            }
            var assignments = system.getAssignmentManager().findByRole(role.get());
            if (assignments.isEmpty()) {
                System.out.println("Пользователей с этой ролью нет.");
            } else {
                System.out.println("Пользователи с ролью " + roleName + ":");
                for (var a : assignments) {
                    System.out.println("  - " + a.user().username() + " [" + (a.isActive() ? "ACTIVE" : "INACTIVE") + "]");
                }
            }
        });

        parser.registerCommand("assignment-active", "Только активные назначения", (scanner, system) -> {
            var active = system.getAssignmentManager().getActiveAssignments();
            if (active.isEmpty()) {
                System.out.println("Активных назначений нет.");
            } else {
                System.out.println("Активные назначения (" + active.size() + "):");
                for (var a : active) {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName() + " [" + a.assignmentType() + "]");
                }
            }
        });

        parser.registerCommand("assignment-expired", "Истёкшие временные назначения", (scanner, system) -> {
            var expired = system.getAssignmentManager().getExpiredAssignments();
            if (expired.isEmpty()) {
                System.out.println("Истёкших назначений нет.");
            } else {
                System.out.println("Истёкшие назначения (" + expired.size() + "):");
                for (var a : expired) {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName());
                }
            }
        });

        parser.registerCommand("assignment-extend", "Продлить временное назначение", (scanner, system) -> {
            System.out.print("Assignment ID: ");
            String id = scanner.nextLine().trim();
            System.out.print("Новая дата окончания (yyyy-MM-dd HH:mm): ");
            String newDate = scanner.nextLine().trim();
            try {
                system.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                System.out.println("Назначение продлено до " + newDate);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам", (scanner, system) -> {
            System.out.println("Фильтры:");
            System.out.println("1. По пользователю");
            System.out.println("2. По роли");
            System.out.println("3. По типу (постоянное/временное)");
            System.out.println("4. По статусу (активное/неактивное)");
            System.out.print("Выбор: ");
            String choice = scanner.nextLine().trim();
            AssignmentFilter filter;
            switch (choice) {
                case "1" -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    filter = AssignmentFilters.byUsername(username);
                }
                case "2" -> {
                    System.out.print("Имя роли: ");
                    String roleName = scanner.nextLine().trim();
                    filter = AssignmentFilters.byRoleName(roleName);
                }
                case "3" -> {
                    System.out.print("Тип (PERMANENT или TEMPORARY): ");
                    String type = scanner.nextLine().trim().toUpperCase();
                    filter = AssignmentFilters.byType(type);
                }
                case "4" -> {
                    System.out.print("Статус (1-активное, 2-неактивное): ");
                    String status = scanner.nextLine().trim();
                    filter = "1".equals(status) ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }
            var results = system.getAssignmentManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size());
                for (var a : results) {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName() + " [" + a.assignmentType() + ", " + (a.isActive() ? "ACTIVE" : "INACTIVE") + "]");
                }
            }
        });
    }

    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "Все права конкретного пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            var permissions = system.getAssignmentManager().getUserPermissions(user.get());
            if (permissions.isEmpty()) {
                System.out.println("У пользователя нет прав.");
            } else {
                var grouped = new java.util.TreeMap<String, java.util.List<Permission>>();
                for (Permission p : permissions) {
                    grouped.computeIfAbsent(p.resource(), k -> new java.util.ArrayList<>()).add(p);
                }
                System.out.println("Права пользователя " + username + " (" + permissions.size() + "):");
                for (var entry : grouped.entrySet()) {
                    System.out.println("  Ресурс: " + entry.getKey());
                    for (Permission p : entry.getValue()) {
                        System.out.println("    - " + p.name() + ": " + p.description());
                    }
                }
            }
        });

        parser.registerCommand("permissions-check", "Проверить право пользователя", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            System.out.print("Имя права: ");
            String permissionName = scanner.nextLine().trim();
            System.out.print("Ресурс: ");
            String resource = scanner.nextLine().trim();
            boolean has = system.getAssignmentManager().userHasPermission(user.get(), permissionName, resource);
            if (has) {
                System.out.println("У пользователя есть это право.");
                var assignments = system.getAssignmentManager().findByUser(user.get()).stream()
                        .filter(RoleAssignment::isActive)
                        .filter(a -> a.role().hasPermission(permissionName, resource))
                        .toList();
                System.out.println("Из ролей:");
                for (var a : assignments) {
                    System.out.println("  - " + a.role().getName());
                }
            } else {
                System.out.println("У пользователя нет этого права.");
            }
        });
    }

    private static void registerServiceCommands(CommandParser parser) {
        parser.registerCommand("help", "Справка по командам", (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "Статистика системы", (scanner, system) -> {
            System.out.println(system.generateStatistics());
        });

        parser.registerCommand("clear", "Очистить экран", (scanner, system) -> {
            for (int i = 0; i < 50; i++) System.out.println();
        });

        parser.registerCommand("exit", "Выход из программы", (scanner, system) -> {
            System.out.print("Выйти? (да/нет): ");
            if ("да".equalsIgnoreCase(scanner.nextLine().trim())) {
                System.out.println("До свидания!");
                System.exit(0);
            }
        });
    }
}
