import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

final class RoleManager implements Repository<Role> {

    private final ConcurrentMap<String, Role> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Role> byName = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private java.util.function.Predicate<Role> removeGuard;

    void setRemoveGuard(java.util.function.Predicate<Role> guard) {
        this.removeGuard = guard;
    }

    @Override
    public void add(Role item) {
        if (item == null) throw new IllegalArgumentException("Role не может быть null");

        lock.writeLock().lock();
        try {
            if (byName.containsKey(item.getName()))
                throw new IllegalArgumentException("Роль с именем '" + item.getName() + "' уже существует");
            byId.put(item.getId(), item);
            byName.put(item.getName(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Role item) {
        if (item == null) return false;

        lock.writeLock().lock();
        try {
            if (removeGuard != null && removeGuard.test(item))
                throw new IllegalStateException("Роль '" + item.getName() + "' назначена пользователям");
            var r = byId.remove(item.getId());
            if (r != null) byName.remove(r.getName());
            return r != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(byId.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(byId.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return byId.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            byId.clear();
            byName.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    Optional<Role> findByName(String name) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(byName.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) return findAll();
        lock.readLock().lock();
        try {
            return byId.values().stream()
                    .filter(filter::test)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        var list = filter != null ? findByFilter(filter) : findAll();
        return sorter != null ? list.stream().sorted(sorter).toList() : list;
    }

    boolean exists(String name) {
        if (name == null) return false;
        lock.readLock().lock();
        try {
            return byName.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            var role = findByName(roleName).orElseThrow(() ->
                    new IllegalArgumentException("Роль '" + roleName + "' не найдена"));
            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void removePermissionFromRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            var role = findByName(roleName).orElseThrow(() ->
                    new IllegalArgumentException("Роль '" + roleName + "' не найдена"));
            role.removePermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    List<Role> findRolesWithPermission(String permissionName, String resource) {
        return findByFilter(RoleFilters.hasPermission(permissionName, resource));
    }

    List<Role> findByFilterParallel(RoleFilter filter) {
        if (filter == null) return findAll();
        lock.readLock().lock();
        try {
            return byId.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}