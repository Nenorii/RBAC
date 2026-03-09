public record User(String username, String fullName, String email) {

    public static User create(String username, String fullName, String email) {

        ValidationUtils.requireNonEmpty(username, "username");
        ValidationUtils.requireNonEmpty(fullName, "fullName");
        ValidationUtils.requireNonEmpty(email, "email");

        String normUsername = ValidationUtils.normalizeString(username);
        String normFullName = ValidationUtils.normalizeString(fullName);
        String normEmail = ValidationUtils.normalizeString(email);

        if (!ValidationUtils.isValidUsername(normUsername)) {
            throw new IllegalArgumentException("Недопустимое имя пользователя: " + normUsername);
        }
        if (!ValidationUtils.isValidEmail(normEmail)) {
            throw new IllegalArgumentException("Некорректный формат email: " + normEmail);
        }

        return new User(normUsername, normFullName, normEmail);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}
