import java.util.Objects;

final class UserFilters {
    private UserFilters() {
    }

    static UserFilter byUsername(String username) {
        Objects.requireNonNull(username, "username cannot be null");
        return u -> u != null && username.equals(u.username());
    }

    static UserFilter byUsernameContains(String substring) {
        if (substring == null) return u -> false;
        String sub = substring.toLowerCase();
        return u -> u != null && u.username().toLowerCase().contains(sub);
    }

    static UserFilter byEmail(String email) {
        Objects.requireNonNull(email, "email cannot be null");
        return u -> u != null && email.equals(u.email());
    }

    static UserFilter byEmailDomain(String domain) {
        if (domain == null) return u -> false;
        String d = domain.startsWith("@") ? domain.toLowerCase() : "@" + domain.toLowerCase();
        return u -> u != null && u.email().toLowerCase().endsWith(d);
    }

    static UserFilter byFullNameContains(String substring) {
        if (substring == null) return u -> false;
        String sub = substring.toLowerCase();
        return u -> u != null && u.fullName().toLowerCase().contains(sub);
    }
}
