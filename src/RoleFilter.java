@FunctionalInterface
interface RoleFilter {
    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        return r -> test(r) && other.test(r);
    }

    default RoleFilter or(RoleFilter other) {
        return r -> test(r) || other.test(r);
    }
}
