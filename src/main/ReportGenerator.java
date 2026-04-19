import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Report\n");

        var users = userManager.findAll();
        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }

        for (User u : users) {
            sb.append(String.format("User: %s (%s) <%s>%n",
                    u.username(), u.fullName(), u.email()));
            var assignments = assignmentManager.findByUser(u);
            if (assignments.isEmpty()) {
                sb.append("  Roles: none\n");
            } else {
                sb.append("  Roles:\n");
                for (RoleAssignment a : assignments) {
                    sb.append(String.format("    - %s [%s, %s]%n",
                            a.role().getName(),
                            a.assignmentType(),
                            a.isActive() ? "ACTIVE" : "INACTIVE"));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Report (PARALLEL)\n");
        sb.append("=".repeat(50)).append("\n");

        var users = userManager.findAll();
        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }

        var userReports = users.parallelStream()
                .map(u -> {
                    StringBuilder userSb = new StringBuilder();
                    userSb.append(String.format("User: %s (%s) <%s>%n",
                            u.username(), u.fullName(), u.email()));

                    var assignments = assignmentManager.findByUser(u);
                    if (assignments.isEmpty()) {
                        userSb.append("  Roles: none\n");
                    } else {
                        userSb.append("  Roles:\n");
                        for (RoleAssignment a : assignments) {
                            userSb.append(String.format("    - %s [%s, %s]%n",
                                    a.role().getName(),
                                    a.assignmentType(),
                                    a.isActive() ? "ACTIVE" : "INACTIVE"));
                        }
                    }
                    userSb.append("\n");
                    return userSb.toString();
                })
                .collect(Collectors.toList());

        for (String report : userReports) {
            sb.append(report);
        }

        sb.append("Generated with parallelStream using ")
                .append(Runtime.getRuntime().availableProcessors())
                .append(" processors\n");

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role Report\n");

        var roles = roleManager.findAll();
        if (roles.isEmpty()) {
            sb.append("No roles.\n");
            return sb.toString();
        }

        for (Role r : roles) {
            var assignments = assignmentManager.findByRole(r);
            long usersCount = assignments.stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            sb.append(String.format("Role: %s (id=%s)%n", r.getName(), r.getId()));
            sb.append(String.format("  Users: %d%n", usersCount));
            sb.append(String.format("  Permissions: %d%n", r.getPermissions().size()));
            sb.append("\n");
        }
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission Matrix (Users x Resources)\n");

        var users = userManager.findAll();
        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }

        Set<String> resources = new TreeSet<>();
        Map<String, Set<String>> userToResources = new TreeMap<>();

        for (User u : users) {
            var perms = assignmentManager.getUserPermissions(u);
            Set<String> resForUser = perms.stream()
                    .map(Permission::resource)
                    .collect(Collectors.toSet());
            userToResources.put(u.username(), resForUser);
            resources.addAll(resForUser);
        }

        if (resources.isEmpty()) {
            sb.append("No permissions assigned.\n");
            return sb.toString();
        }

        sb.append(String.format("%-20s", "User"));
        for (String res : resources) {
            sb.append(String.format(" | %-15s", res));
        }
        sb.append("\n");

        sb.append("-".repeat(20 + (resources.size() * 18)));
        sb.append("\n");

        for (User u : users) {
            sb.append(String.format("%-20s", u.username()));
            Set<String> resForUser = userToResources.getOrDefault(u.username(), Set.of());
            for (String res : resources) {
                String mark = resForUser.contains(res) ? "X" : "";
                sb.append(String.format(" | %-15s", mark));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission Matrix (Users x Resources) - PARALLEL\n");
        sb.append("=".repeat(60)).append("\n");

        var users = userManager.findAll();
        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }

        Map<String, Set<String>> userToResources = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        User::username,
                        u -> assignmentManager.getUserPermissions(u).stream()
                                .map(Permission::resource)
                                .collect(Collectors.toSet())
                ));

        Set<String> resources = userToResources.values().parallelStream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        if (resources.isEmpty()) {
            sb.append("No permissions assigned.\n");
            return sb.toString();
        }

        sb.append(String.format("%-20s", "User"));
        for (String res : resources) {
            sb.append(String.format(" | %-15s", res));
        }
        sb.append("\n");

        sb.append("-".repeat(20 + (resources.size() * 18)));
        sb.append("\n");

        for (User u : users) {
            sb.append(String.format("%-20s", u.username()));
            Set<String> resForUser = userToResources.getOrDefault(u.username(), Set.of());
            for (String res : resources) {
                String mark = resForUser.contains(res) ? "X" : "";
                sb.append(String.format(" | %-15s", mark));
            }
            sb.append("\n");
        }

        sb.append("\nGenerated with parallelStream using ")
                .append(Runtime.getRuntime().availableProcessors())
                .append(" processors\n");

        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        try (java.io.BufferedWriter writer =
                     new java.io.BufferedWriter(new java.io.FileWriter(filename))) {
            writer.write(report);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Ошибка при сохранении отчёта в файл: " + filename, e);
        }
    }
}