import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

final class AssignmentManager implements Repository<RoleAssignment> {

    private final ConcurrentMap<String, RoleAssignment> storage = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final UserManager userManager;
    private final RoleManager roleManager;

    AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("RoleAssignment не может быть null");

        lock.writeLock().lock();
        try {
            if (!userManager.exists(item.user().username()))
                throw new IllegalArgumentException("Пользователь '" + item.user().username() + "' не найден");
            if (!roleManager.exists(item.role().getName()))
                throw new IllegalArgumentException("Роль '" + item.role().getName() + "' не найдена");
            if (userHasRole(item.user(), item.role()))
                throw new IllegalStateException("Роль '" + item.role().getName() + "' уже назначена пользователю '" + item.user().username() + "'");
            storage.put(item.assignmentId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) return false;
        lock.writeLock().lock();
        try {
            return storage.remove(item.assignmentId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(storage.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(storage.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return storage.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            storage.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    List<RoleAssignment> findByUser(User user) {
        return findByFilter(AssignmentFilters.byUser(user));
    }

    List<RoleAssignment> findByRole(Role role) {
        return findByFilter(AssignmentFilters.byRole(role));
    }

    List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) return findAll();
        lock.readLock().lock();
        try {
            return storage.values().stream()
                    .filter(filter::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .anyMatch(a -> a.role().equals(role));
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream()
                .anyMatch(p -> p.matches(permissionName, resource));
    }

    Set<Permission> getUserPermissions(User user) {
        lock.readLock().lock();
        try {
            var perms = new HashSet<Permission>();
            findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getPermissions())
                    .forEach(perms::addAll);
            return perms;
        } finally {
            lock.readLock().unlock();
        }
    }

    void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            var a = findById(assignmentId).orElseThrow(() ->
                    new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));
            if (a instanceof PermanentAssignment pa) pa.revoke();
            else if (a instanceof TemporaryAssignment ta) ta.extend("1970-01-01");
        } finally {
            lock.writeLock().unlock();
        }
    }

    void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            var a = findById(assignmentId).orElseThrow(() ->
                    new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));
            if (!(a instanceof TemporaryAssignment ta))
                throw new IllegalArgumentException("Назначение '" + assignmentId + "' не временное");
            ta.extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, int days) {
        lock.writeLock().lock();
        try {
            var assignmentOpt = findById(assignmentId);
            var assignment = assignmentOpt.orElseThrow(() ->
                    new IllegalArgumentException("Назначение '" + assignmentId + "' не найдено"));

            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalArgumentException("Только временные назначения можно продлить");
            }

            String currentExpiry = temp.getExpiresAt();
            String newExpiry = DateUtils.extend(currentExpiry, days);
            temp.extend(newExpiry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        if (filter == null) return findAll();
        lock.readLock().lock();
        try {
            return storage.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}