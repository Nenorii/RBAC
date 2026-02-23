@FunctionalInterface
interface UserFilter {
    boolean test(User user);

    default UserFilter and(UserFilter other) {
        return u -> test(u) && other.test(u);
    }

    default UserFilter or(UserFilter other) {
        return u -> test(u) || other.test(u);
    }
}
