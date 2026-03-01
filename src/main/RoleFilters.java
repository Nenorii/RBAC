import java.util.Objects;

final class RoleFilters {
    private RoleFilters() {}

    static RoleFilter byName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return r -> r != null && name.equals(r.getName());
    }

    static RoleFilter byNameContains(String substring) {
        if (substring == null) return r -> false;
        String sub = substring.toLowerCase();
        return r -> r != null && r.getName().toLowerCase().contains(sub);
    }

    static RoleFilter hasPermission(Permission permission) {
        Objects.requireNonNull(permission, "permission cannot be null");
        return r -> r != null && r.hasPermission(permission);
    }

    static RoleFilter hasPermission(String permissionName, String resource) {
        Objects.requireNonNull(permissionName, "permissionName cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        return r -> r != null && r.hasPermission(permissionName, resource);
    }

    static RoleFilter hasAtLeastNPermissions(int n) {
        return r -> r != null && r.getPermissions().size() >= n;
    }
}