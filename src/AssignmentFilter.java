@FunctionalInterface
interface AssignmentFilter {
    boolean test(RoleAssignment assignment);

    default AssignmentFilter and(AssignmentFilter other) {
        return a -> test(a) && other.test(a);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        return a -> test(a) || other.test(a);
    }
}
