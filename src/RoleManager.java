import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

final class RoleManager implements Repository<Role> {

    private final Map<String, Role> byId = new TreeMap<>();
    private final Map<String, Role> byName = new TreeMap<>();

    private java.util.function.Predicate<Role> removeGuard;

    void setRemoveGuard(java.util.function.Predicate<Role> guard) {
        this.removeGuard = guard;
    }

    @Override
    public void add(Role item) {
        if (item == null) throw new IllegalArgumentException("Role не может быть null");
        if (byName.containsKey(item.getName()))
            throw new IllegalArgumentException("Роль с именем '" + item.getName() + "' уже существует");
        byId.put(item.getId(), item);
        byName.put(item.getName(), item);
    }

    @Override
    public boolean remove(Role item) {
        if (item == null) return false;
        if (removeGuard != null && removeGuard.test(item))
            throw new IllegalStateException("Роль '" + item.getName() + "' назначена пользователям");
        var r = byId.remove(item.getId());
        if (r != null) byName.remove(r.getName());
        return r != null;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public int count() {
        return byId.size();
    }

    @Override
    public void clear() {
        byId.clear();
        byName.clear();
    }

    Optional<Role> findByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) return findAll();
        return byId.values().stream().filter(filter::test).toList();
    }

    List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        var list = filter != null ? findByFilter(filter) : findAll();
        return sorter != null ? list.stream().sorted(sorter).toList() : list;
    }

    boolean exists(String name) {
        return name != null && byName.containsKey(name);
    }

    void addPermissionToRole(String roleName, Permission permission) {
        var role = findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Роль '" + roleName + "' не найдена"));
        role.addPermission(permission);
    }

    void removePermissionFromRole(String roleName, Permission permission) {
        var role = findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Роль '" + roleName + "' не найдена"));
        role.removePermission(permission);
    }

    List<Role> findRolesWithPermission(String permissionName, String resource) {
        return findByFilter(RoleFilters.hasPermission(permissionName, resource));
    }
}