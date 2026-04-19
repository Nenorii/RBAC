import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class UserManager implements Repository<User> {

    private final ConcurrentMap<String, User> storage = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(User item) {
        if (item == null) throw new IllegalArgumentException("User не может быть null");
        User.create(item.username(), item.fullName(), item.email());

        lock.writeLock().lock();
        try {
            if (storage.containsKey(item.username()))
                throw new IllegalArgumentException("Пользователь с username '" + item.username() + "' уже существует");
            storage.put(item.username(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User item) {
        if (item == null) return false;
        lock.writeLock().lock();
        try {
            return storage.remove(item.username()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(storage.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
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

    Optional<User> findByUsername(String username) {
        return findById(username);
    }

    Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        lock.readLock().lock();
        try {
            return storage.values().stream()
                    .filter(u -> email.equals(u.email()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    List<User> findByFilter(UserFilter filter) {
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

    List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        var list = filter != null ? findByFilter(filter) : findAll();
        return sorter != null ? list.stream().sorted(sorter).toList() : list;
    }

    boolean exists(String username) {
        if (username == null) return false;
        lock.readLock().lock();
        try {
            return storage.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    void update(String username, String newFullName, String newEmail) {
        lock.writeLock().lock();
        try {
            var user = findById(username).orElseThrow(() ->
                    new IllegalArgumentException("Пользователь '" + username + "' не найден"));
            User validated = User.create(username, newFullName, newEmail);
            var byEmail = findByEmail(newEmail);
            if (byEmail.isPresent() && !byEmail.get().username().equals(username))
                throw new IllegalArgumentException("Email '" + newEmail + "' уже используется");
            storage.put(username, validated);
        } finally {
            lock.writeLock().unlock();
        }
    }
}