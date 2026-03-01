import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

final class AssignmentFilters {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private AssignmentFilters() {}

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return LocalDateTime.parse(s);
            return s.length() > 10 ?
                    LocalDateTime.parse(s, FMT) :
                    LocalDateTime.parse(s + " 23:59", FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static AssignmentFilter byUser(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        return a -> a != null && user.equals(a.user());
    }

    static AssignmentFilter byUsername(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        return a -> a != null && username.equals(a.user().username());
    }

    static AssignmentFilter byRole(Role role) {
        Objects.requireNonNull(role, "role cannot be null");
        return a -> a != null && role.equals(a.role());
    }

    static AssignmentFilter byRoleName(String roleName) {
        Objects.requireNonNull(roleName, "roleName cannot be null");
        return a -> a != null && roleName.equals(a.role().getName());
    }

    static AssignmentFilter activeOnly() {
        return a -> a != null && a.isActive();
    }

    static AssignmentFilter inactiveOnly() {
        return a -> a != null && !a.isActive();
    }

    static AssignmentFilter byType(String type) {
        Objects.requireNonNull(type, "type cannot be null");
        return a -> a != null && type.equalsIgnoreCase(a.assignmentType());
    }

    static AssignmentFilter assignedBy(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        return a -> a != null && username.equals(a.metadata().assignedBy());
    }

    static AssignmentFilter assignedAfter(String date) {
        Objects.requireNonNull(date, "date cannot be null");
        LocalDateTime after = parseDate(date);
        if (after == null) return a -> false;
        return a -> {
            LocalDateTime at = parseDate(a.metadata().assignedAt());
            return at != null && at.isAfter(after);
        };
    }

    static AssignmentFilter expiringBefore(String date) {
        Objects.requireNonNull(date, "date cannot be null");
        LocalDateTime before = parseDate(date);
        if (before == null) return a -> false;
        return a -> {
            if (!(a instanceof TemporaryAssignment ta)) return false;
            String exp = ta.getExpiresAt();
            LocalDateTime end = parseDate(exp);
            return end != null && end.isBefore(before);
        };
    }
}