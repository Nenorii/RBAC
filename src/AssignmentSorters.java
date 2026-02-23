import java.util.Comparator;

final class AssignmentSorters {
    private AssignmentSorters() {}

    static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(a -> a.user().username(), String.CASE_INSENSITIVE_ORDER);
    }

    static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(a -> a.role().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(a -> a.metadata().assignedAt());
    }
}
