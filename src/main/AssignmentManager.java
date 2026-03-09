import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

final class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> storage = new TreeMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("RoleAssignment не может быть null");
        if (!userManager.exists(item.user().username()))
            throw new IllegalArgumentException("Пользователь '" + item.user().username() + "' не найден");
        if (!roleManager.exists(item.role().getName()))
            throw new IllegalArgumentException("Роль '" + item.role().getName() + "' не найдена");
        if (userHasRole(item.user(), item.role()))
            throw new IllegalStateException("Роль '" + item.role().getName() + "' уже назначена пользователю '" + item.user().username() + "'");
        storage.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        return item != null && storage.remove(item.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public int count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }

    List<RoleAssignment> findByUser(User user) {
        return findByFilter(AssignmentFilters.byUser(user));
    }

    List<RoleAssignment> findByRole(Role role) {
        return findByFilter(AssignmentFilters.byRole(role));
    }

    List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) return findAll();
        return storage.values().stream().filter(filter::test).toList();
    }

    List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        var list = filter != null ? findByFilter(filter) : findAll();
        return sorter != null ? list.stream().sorted(sorter).toList() : list;
    }

    List<RoleAssignment> getActiveAssignments() {
        return findByFilter(AssignmentFilters.activeOnly());
    }

    List<RoleAssignment> getExpiredAssignments() {
        return findByFilter(AssignmentFilters.inactiveOnly());
    }

    boolean userHasRole(User user, Role role) {
        return findByUser(user).stream().filter(RoleAssignment::isActive).anyMatch(a -> a.role().equals(role));
    }

    boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream().anyMatch(p -> p.matches(permissionName, resource));
    }

    Set<Permission> getUserPermissions(User user) {
        var perms = new HashSet<Permission>();
        findByUser(user).stream().filter(RoleAssignment::isActive).map(a -> a.role().getPermissions()).forEach(perms::addAll);
        return perms;
    }

    void revokeAssignment(String assignmentId) {
        var a = findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));
        if (a instanceof PermanentAssignment pa) pa.revoke();
        else if (a instanceof TemporaryAssignment ta) ta.extend("1970-01-01");
    }

    void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        var a = findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));
        if (!(a instanceof TemporaryAssignment ta))
            throw new IllegalArgumentException("Назначение '" + assignmentId + "' не временное");
        ta.extend(newExpirationDate);
    }

    public void extendTemporaryAssignment(String assignmentId, int days) {
        var assignmentOpt = findById(assignmentId);
        var assignment = assignmentOpt.orElseThrow(() ->
                new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));

        if (!(assignment instanceof TemporaryAssignment temp)) {
            throw new IllegalArgumentException("Только временные назначения можно продлить");
        }

        String currentExpiry = temp.getExpiresAt();
        String newExpiry = DateUtils.extend(currentExpiry, days);
        temp.extend(newExpiry);
    }
}