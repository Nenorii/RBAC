import java.util.Comparator;

final class UserSorters {
    private UserSorters() {}

    static Comparator<User> byUsername() {
        return Comparator.comparing(User::username);
    }

    static Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName, String.CASE_INSENSITIVE_ORDER);
    }

    static Comparator<User> byEmail() {
        return Comparator.comparing(User::email, String.CASE_INSENSITIVE_ORDER);
    }
}
