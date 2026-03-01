import java.util.Comparator;

final class RoleSorters {
    private RoleSorters() {}

    static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER);
    }

    static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(r -> r.getPermissions().size());
    }
}