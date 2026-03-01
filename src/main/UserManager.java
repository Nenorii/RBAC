import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

final class UserManager implements Repository<User> {

    private final Map<String, User> storage = new TreeMap<>();

    @Override
    public void add(User item) {
        if (item == null) throw new IllegalArgumentException("User не может быть null");
        User.create(item.username(), item.fullName(), item.email());
        if (storage.containsKey(item.username()))
            throw new IllegalArgumentException("Пользователь с username '" + item.username() + "' уже существует");
        storage.put(item.username(), item);
    }

    @Override
    public boolean remove(User item) {
        return item != null && storage.remove(item.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findAll() {
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

    Optional<User> findByUsername(String username) {
        return findById(username);
    }

    Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return storage.values().stream().filter(u -> email.equals(u.email())).findFirst();
    }

    List<User> findByFilter(UserFilter filter) {
        if (filter == null) return findAll();
        return storage.values().stream().filter(filter::test).toList();
    }

    List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        var list = filter != null ? findByFilter(filter) : findAll();
        return sorter != null ? list.stream().sorted(sorter).toList() : list;
    }

    boolean exists(String username) {
        return username != null && storage.containsKey(username);
    }

    void update(String username, String newFullName, String newEmail) {
        var user = findById(username).orElseThrow(() -> new IllegalArgumentException("Пользователь '" + username + "' не найден"));
        User validated = User.create(username, newFullName, newEmail);
        var byEmail = findByEmail(newEmail);
        if (byEmail.isPresent() && !byEmail.get().username().equals(username))
            throw new IllegalArgumentException("Email '" + newEmail + "' уже используется");
        storage.put(username, validated);
    }
}