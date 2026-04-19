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
        registerAuditCommands(parser);
        registerReportCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "Список всех пользователей", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();
            if (users.isEmpty()) {
                System.out.println("Пользователей нет.");
                return;
            }

            String[] headers = {"Username", "Полное имя", "Email"};
            List<String[]> rows = users.stream()
                    .map(u -> new String[]{
                            u.username(),
                            FormatUtils.truncate(u.fullName(), 30),
                            FormatUtils.truncate(u.email(), 30)
                    })
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
        });


        parser.registerCommand("user-create", "Создать нового пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            String fullName = ConsoleUtils.promptString(scanner, "Полное имя", true);
            String email = ConsoleUtils.promptString(scanner, "Email", true);
            try {
                User user = User.create(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("Пользователь создан: " + user.format());

                system.getAuditLog().log(
                        "USER_CREATE",
                        system.getCurrentUser(),
                        user.username(),
                        "fullName=" + user.fullName() + ", email=" + user.email()
                );
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "Просмотр информации о пользователе", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
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
                    System.out.println("  - " + a.role().getName() + " [" + a.assignmentType()
                            + ", " + (a.isActive() ? "ACTIVE" : "INACTIVE") + "]");
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
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            String fullName = ConsoleUtils.promptString(scanner, "Новое полное имя", true);
            String email = ConsoleUtils.promptString(scanner, "Новый email", true);
            try {
                system.getUserManager().update(username, fullName, email);
                System.out.println("Данные обновлены.");

                system.getAuditLog().log(
                        "USER_UPDATE",
                        system.getCurrentUser(),
                        username,
                        "newFullName=" + fullName + ", newEmail=" + email
                );
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Удалить пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Подтвердите удаление");
            if (!confirm) {
                System.out.println("Отменено.");
                return;
            }
            var assignments = system.getAssignmentManager().findByUser(user.get());
            for (var a : assignments) {
                system.getAssignmentManager().remove(a);
            }
            system.getUserManager().remove(user.get());
            System.out.println("Пользователь удалён.");

            system.getAuditLog().log(
                    "USER_DELETE",
                    system.getCurrentUser(),
                    username,
                    "Пользователь и все назначения удалены"
            );
        });

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам (1=обычный, 2=параллельный)", (scanner, system) -> {
            System.out.println("Выберите режим:");
            System.out.println("1. Обычный поиск");
            System.out.println("2. Параллельный поиск (parallelStream)");
            int mode = ConsoleUtils.promptInt(scanner, "Режим", 1, 2);

            System.out.println("Выберите фильтр:");
            System.out.println("1. По username (содержит)");
            System.out.println("2. По email (содержит)");
            System.out.println("3. По домену email");
            System.out.println("4. По полному имени (содержит)");
            int choice = ConsoleUtils.promptInt(scanner, "Выбор", 1, 4);

            UserFilter filter;
            switch (choice) {
                case 1 -> {
                    String sub = ConsoleUtils.promptString(scanner, "Подстрока username", true);
                    filter = UserFilters.byUsernameContains(sub);
                }
                case 2 -> {
                    String sub = ConsoleUtils.promptString(scanner, "Подстрока email", true);
                    filter = user -> user.email().toLowerCase().contains(sub.toLowerCase());
                }
                case 3 -> {
                    String domain = ConsoleUtils.promptString(scanner, "Домен (@example.com)", true);
                    filter = UserFilters.byEmailDomain(domain);
                }
                case 4 -> {
                    String sub = ConsoleUtils.promptString(scanner, "Подстрока полного имени", true);
                    filter = UserFilters.byFullNameContains(sub);
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }

            long startTime = System.currentTimeMillis();
            List<User> results;
            if (mode == 2) {
                results = system.getUserManager().findByFilterParallel(filter);
                System.out.println("Использован параллельный поиск");
            } else {
                results = system.getUserManager().findByFilter(filter);
                System.out.println("Использован обычный поиск");
            }
            long endTime = System.currentTimeMillis();

            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size() + " (за " + (endTime - startTime) + " мс)");
                for (User u : results) {
                    System.out.println("  " + u.format());
                }
            }

            system.getAuditLog().log(
                    "USER_SEARCH",
                    system.getCurrentUser(),
                    "filter=" + choice,
                    "mode=" + (mode == 2 ? "parallel" : "sequential") + ", found=" + results.size()
            );
        });
    }

    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "Список всех ролей", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("Ролей нет.");
                return;
            }

            String[] headers = {"Название", "Прав", "ID"};
            List<String[]> rows = roles.stream()
                    .map(r -> new String[]{
                            FormatUtils.truncate(r.getName(), 20),
                            String.valueOf(r.getPermissions().size()),
                            r.getId()
                    })
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
        });


        parser.registerCommand("role-create", "Создать новую роль", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Название роли", true);
            String desc = ConsoleUtils.promptString(scanner, "Описание", false);
            try {
                Role role = new Role(name, desc);
                system.getRoleManager().add(role);
                System.out.println("Роль создана: " + role.getName() + " [" + role.getId() + "]");

                system.getAuditLog().log(
                        "ROLE_CREATE",
                        system.getCurrentUser(),
                        role.getName(),
                        "id=" + role.getId()
                );

                if (ConsoleUtils.promptYesNo(scanner, "Добавить права?")) {
                    while (true) {
                        String pName = ConsoleUtils.promptString(scanner, "Имя права (пусто=завершить)", false);
                        if (pName.isBlank()) break;
                        String resource = ConsoleUtils.promptString(scanner, "Ресурс", true);
                        String pDesc = ConsoleUtils.promptString(scanner, "Описание права", false);
                        Permission p = new Permission(pName, resource, pDesc);
                        role.addPermission(p);
                        System.out.println("Право добавлено.");

                        system.getAuditLog().log(
                                "ROLE_ADD_PERMISSION",
                                system.getCurrentUser(),
                                role.getName(),
                                "permission=" + p.format()
                        );
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "Просмотр роли", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Имя роли", true);
            var role = system.getRoleManager().findByName(name);
            if (role.isEmpty()) {
                System.out.println("Роль не найдена.");
                return;
            }
            System.out.println("\n" + role.get().format());
        });

        parser.registerCommand("role-delete", "Удалить роль", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Имя роли", true);
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
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Подтвердите удаление");
            if (!confirm) {
                System.out.println("Отменено.");
                return;
            }
            try {
                system.getRoleManager().remove(role.get());
                System.out.println("Роль удалена.");

                system.getAuditLog().log(
                        "ROLE_DELETE",
                        system.getCurrentUser(),
                        name,
                        "Роль удалена"
                );
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);
            String pName = ConsoleUtils.promptString(scanner, "Имя права", true);
            String resource = ConsoleUtils.promptString(scanner, "Ресурс", true);
            String desc = ConsoleUtils.promptString(scanner, "Описание", false);
            try {
                Permission p = new Permission(pName, resource, desc);
                system.getRoleManager().addPermissionToRole(roleName, p);
                System.out.println("Право добавлено к роли.");

                system.getAuditLog().log(
                        "ROLE_ADD_PERMISSION",
                        system.getCurrentUser(),
                        roleName,
                        "permission=" + p.format()
                );
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);
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
            Permission perm = ConsoleUtils.promptChoice(scanner, "Права роли", perms);
            system.getRoleManager().removePermissionFromRole(roleName, perm);
            System.out.println("Право удалено.");

            system.getAuditLog().log(
                    "ROLE_REMOVE_PERMISSION",
                    system.getCurrentUser(),
                    roleName,
                    "permission=" + perm.format()
            );
        });

        parser.registerCommand("role-search", "Поиск ролей (1=обычный, 2=параллельный)", (scanner, system) -> {
            System.out.println("Выберите режим:");
            System.out.println("1. Обычный поиск");
            System.out.println("2. Параллельный поиск (parallelStream)");
            int mode = ConsoleUtils.promptInt(scanner, "Режим", 1, 2);

            System.out.println("Поиск ролей:");
            System.out.println("1. По имени (содержит)");
            System.out.println("2. По наличию конкретного права");
            System.out.println("3. По минимальному количеству прав");
            int choice = ConsoleUtils.promptInt(scanner, "Выбор", 1, 3);

            RoleFilter filter;
            switch (choice) {
                case 1 -> {
                    String sub = ConsoleUtils.promptString(scanner, "Подстрока имени", true);
                    filter = RoleFilters.byNameContains(sub);
                }
                case 2 -> {
                    String pName = ConsoleUtils.promptString(scanner, "Имя права", true);
                    String resource = ConsoleUtils.promptString(scanner, "Ресурс", true);
                    filter = RoleFilters.hasPermission(pName, resource);
                }
                case 3 -> {
                    int n = ConsoleUtils.promptInt(scanner, "Минимум прав", 1, 100);
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }

            long startTime = System.currentTimeMillis();
            List<Role> results;
            if (mode == 2) {
                results = system.getRoleManager().findByFilterParallel(filter);
                System.out.println("Использован параллельный поиск");
            } else {
                results = system.getRoleManager().findByFilter(filter);
                System.out.println("Использован обычный поиск");
            }
            long endTime = System.currentTimeMillis();

            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size() + " (за " + (endTime - startTime) + " мс)");
                for (Role r : results) {
                    System.out.println("  " + r.getName() + " (" + r.getPermissions().size() + " прав)");
                }
            }

            system.getAuditLog().log(
                    "ROLE_SEARCH",
                    system.getCurrentUser(),
                    "filter=" + choice,
                    "mode=" + (mode == 2 ? "parallel" : "sequential") + ", found=" + results.size()
            );
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Назначить роль пользователю", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
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
            Role role = ConsoleUtils.promptChoice(scanner, "Доступные роли", roles);
            int type = ConsoleUtils.promptInt(scanner, "Тип (1=постоянное, 2=временное)", 1, 2);
            String reason = ConsoleUtils.promptString(scanner, "Причина назначения", true);
            AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), reason);
            try {
                if (type == 1) {
                    PermanentAssignment pa = new PermanentAssignment(user.get(), role, meta);
                    system.getAssignmentManager().add(pa);
                    System.out.println("Роль назначена (постоянно).");

                    system.getAuditLog().log(
                            "ROLE_ASSIGN",
                            system.getCurrentUser(),
                            user.get().username(),
                            "role=" + role.getName() + ", type=PERMANENT"
                    );
                } else {
                    String expiresAt = ConsoleUtils.promptString(scanner, "Дата окончания (yyyy-MM-dd HH:mm)", true);
                    TemporaryAssignment ta = new TemporaryAssignment(user.get(), role, meta, expiresAt);
                    system.getAssignmentManager().add(ta);
                    System.out.println("Роль назначена (временно до " + expiresAt + ").");

                    system.getAuditLog().log(
                            "ROLE_ASSIGN",
                            system.getCurrentUser(),
                            user.get().username(),
                            "role=" + role.getName() + ", type=TEMPORARY, until=" + expiresAt
                    );
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
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
            RoleAssignment assignment = ConsoleUtils.promptChoice(scanner, "Активные назначения", assignments);
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Подтвердите отзыв роли");
            if (!confirm) {
                System.out.println("Отменено.");
                return;
            }
            try {
                system.getAssignmentManager().revokeAssignment(assignment.assignmentId());
                System.out.println("Назначение отозвано.");

                system.getAuditLog().log(
                        "ROLE_REVOKE",
                        system.getCurrentUser(),
                        user.get().username(),
                        "role=" + assignment.role().getName() + ", type=" + assignment.assignmentType()
                );
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

            String[] headers = {"Username", "Роль", "Тип", "Статус", "Назначено"};
            List<String[]> rows = all.stream()
                    .map(a -> new String[]{
                            FormatUtils.truncate(a.user().username(), 20),
                            FormatUtils.truncate(a.role().getName(), 20),
                            a.assignmentType(),
                            a.isActive() ? "ACTIVE" : "INACTIVE",
                            FormatUtils.truncate(a.metadata().assignedAt(), 30)
                    })
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
        });


        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
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
            String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);
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
            String id = ConsoleUtils.promptString(scanner, "Assignment ID", true);
            int days = ConsoleUtils.promptInt(scanner, "Дней продлить", 1, 365);
            try {
                system.getAssignmentManager().extendTemporaryAssignment(id, days);
                System.out.println("Назначение продлено на " + days + " дней");

                system.getAuditLog().log(
                        "ASSIGNMENT_EXTEND",
                        system.getCurrentUser(),
                        id,
                        "days=" + days
                );
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });


        parser.registerCommand("assignment-search", "Поиск назначений (1=обычный, 2=параллельный)", (scanner, system) -> {
            System.out.println("Выберите режим:");
            System.out.println("1. Обычный поиск");
            System.out.println("2. Параллельный поиск (parallelStream)");
            int mode = ConsoleUtils.promptInt(scanner, "Режим", 1, 2);

            System.out.println("Фильтры:");
            System.out.println("1. По пользователю");
            System.out.println("2. По роли");
            System.out.println("3. По типу (постоянное/временное)");
            System.out.println("4. По статусу (активное/неактивное)");
            int choice = ConsoleUtils.promptInt(scanner, "Выбор", 1, 4);

            AssignmentFilter filter;
            switch (choice) {
                case 1 -> {
                    String username = ConsoleUtils.promptString(scanner, "Username", true);
                    filter = AssignmentFilters.byUsername(username);
                }
                case 2 -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);
                    filter = AssignmentFilters.byRoleName(roleName);
                }
                case 3 -> {
                    String type = ConsoleUtils.promptString(scanner, "Тип (PERMANENT или TEMPORARY)", true).toUpperCase();
                    filter = AssignmentFilters.byType(type);
                }
                case 4 -> {
                    int status = ConsoleUtils.promptInt(scanner, "Статус (1=активное, 2=неактивное)", 1, 2);
                    filter = status == 1 ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                }
                default -> {
                    System.out.println("Неверный выбор.");
                    return;
                }
            }

            long startTime = System.currentTimeMillis();
            List<RoleAssignment> results;
            if (mode == 2) {
                results = system.getAssignmentManager().findByFilterParallel(filter);
                System.out.println("Использован параллельный поиск");
            } else {
                results = system.getAssignmentManager().findByFilter(filter);
                System.out.println("Использован обычный поиск");
            }
            long endTime = System.currentTimeMillis();

            if (results.isEmpty()) {
                System.out.println("Ничего не найдено.");
            } else {
                System.out.println("Найдено: " + results.size() + " (за " + (endTime - startTime) + " мс)");
                for (var a : results) {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName()
                            + " [" + a.assignmentType() + ", " + (a.isActive() ? "ACTIVE" : "INACTIVE") + "]");
                }
            }

            system.getAuditLog().log(
                    "ASSIGNMENT_SEARCH",
                    system.getCurrentUser(),
                    "filter=" + choice,
                    "mode=" + (mode == 2 ? "parallel" : "sequential") + ", found=" + results.size()
            );
        });
    }

    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "Все права конкретного пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            var permissions = system.getAssignmentManager().getUserPermissions(user.get());
            if (permissions.isEmpty()) {
                System.out.println("У пользователя нет прав.");
                return;
            }

            var grouped = new java.util.TreeMap<String, java.util.List<Permission>>();
            for (Permission p : permissions) {
                grouped.computeIfAbsent(p.resource(), k -> new java.util.ArrayList<>()).add(p);
            }

            System.out.println(FormatUtils.formatHeader("Права пользователя " + username + " (" + permissions.size() + ")"));
            for (var entry : grouped.entrySet()) {
                String[] headers = {"Право", "Описание"};
                List<String[]> rows = entry.getValue().stream()
                        .map(p -> new String[]{
                                p.name(),
                                FormatUtils.truncate(p.description(), 40)
                        })
                        .toList();
                System.out.println(FormatUtils.formatBox("Ресурс: " + entry.getKey()));
                System.out.println(FormatUtils.formatTable(headers, rows));
            }
        });


        parser.registerCommand("permissions-check", "Проверить право пользователя", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username", true);
            var user = system.getUserManager().findByUsername(username);
            if (user.isEmpty()) {
                System.out.println("Пользователь не найден.");
                return;
            }
            String permissionName = ConsoleUtils.promptString(scanner, "Имя права", true);
            String resource = ConsoleUtils.promptString(scanner, "Ресурс", true);
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
            if (ConsoleUtils.promptYesNo(scanner, "Выйти?")) {
                System.out.println("До свидания!");
                System.exit(0);
            }
        });
    }

    private static void registerAuditCommands(CommandParser parser) {
        parser.registerCommand("audit-log", "Просмотр журнала аудита", (scanner, system) -> {
            system.getAuditLog().printLog();
        });

        parser.registerCommand("audit-save", "Сохранить журнал аудита в файл", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
            try {
                system.getAuditLog().saveToFile(filename);
                System.out.println("Аудит-лог сохранён в файл: " + filename);
            } catch (Exception e) {
                System.out.println("Ошибка при сохранении: " + e.getMessage());
            }
        });
    }

    private static void registerReportCommands(CommandParser parser) {
        parser.registerCommand("report-users", "Отчёт по пользователям", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generateUserReport(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    System.out.println("Отчёт сохранён в " + filename);
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-roles", "Отчёт по ролям", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    System.out.println("Отчёт сохранён в " + filename);
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-matrix", "Матрица прав (пользователь × ресурс)", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    System.out.println("Отчёт сохранён в " + filename);
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-users-parallel", "Отчёт по пользователям (параллельная обработка)", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generateUserReportParallel(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);

            system.getAuditLog().log(
                    "REPORT_USERS_PARALLEL",
                    system.getCurrentUser(),
                    "system",
                    "Сгенерирован параллельный отчёт пользователей"
            );

            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    System.out.println("Отчёт сохранён в " + filename);
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-matrix-parallel", "Матрица прав (параллельная обработка)", (scanner, system) -> {
            String report = system.getReportGenerator()
                    .generatePermissionMatrixParallel(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);

            system.getAuditLog().log(
                    "REPORT_MATRIX_PARALLEL",
                    system.getCurrentUser(),
                    "system",
                    "Сгенерирована параллельная матрица прав"
            );

            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    System.out.println("Отчёт сохранён в " + filename);
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-users-async", "Асинхронный отчёт по пользователям (фоновое выполнение)", (scanner, system) -> {
            System.out.println("Запущена фоновая генерация отчёта...");
            long startTime = System.currentTimeMillis();

            system.getBackgroundExecutor().submit(() -> {
                String report = system.getReportGenerator()
                        .generateUserReport(system.getUserManager(), system.getAssignmentManager());
                long duration = System.currentTimeMillis() - startTime;

                System.out.println("\n[АСИНХРОННЫЙ ОТЧЁТ] Сгенерирован за " + duration + " мс");
                System.out.println(report);

                system.getAuditLog().log(
                        "REPORT_USERS_ASYNC",
                        system.getCurrentUser(),
                        "system",
                        "Асинхронный отчёт, время: " + duration + "ms"
                );
            });

            System.out.println("Задача отправлена в фоновый поток. Используйте другие команды, пока отчёт генерируется.");
        });

        parser.registerCommand("save-async", "Асинхронное сохранение данных в файл", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Имя файла для сохранения", true);

            System.out.println("Запущено фоновое сохранение в " + filename + "...");

            system.getBackgroundExecutor().submit(() -> {
                try {
                    system.getAuditLog().saveToFile(filename);
                    System.out.println("[ASYNC] Данные успешно сохранены в " + filename +
                            " (поток: " + Thread.currentThread().getName() + ")");

                    system.getAuditLog().log(
                            "SAVE_ASYNC",
                            system.getCurrentUser(),
                            filename,
                            "Асинхронное сохранение выполнено"
                    );
                } catch (Exception e) {
                    System.err.println("[ASYNC] Ошибка при сохранении: " + e.getMessage());
                    system.getAuditLog().log(
                            "SAVE_ASYNC_ERROR",
                            system.getCurrentUser(),
                            filename,
                            "Ошибка: " + e.getMessage()
                    );
                }
            });

            System.out.println("Задача сохранения отправлена в фоновый поток.");
        });

        parser.registerCommand("workers-status", "Статус фоновых задач", (scanner, system) -> {
            System.out.println("=== Статус фоновых задач ===");
            System.out.println("Активных задач: " + system.getBackgroundExecutor().getActiveTaskCount());
            System.out.println("Очередь аудит-лога: " + system.getAuditLog().getQueueSize() + " записей");
            System.out.println("=============================");
        });
    }
}
