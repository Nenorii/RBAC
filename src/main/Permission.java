import java.util.Locale;

public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description) {

        ValidationUtils.requireNonEmpty(name, "permission name");
        ValidationUtils.requireNonEmpty(resource, "resource");
        ValidationUtils.requireNonEmpty(description, "description");

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Название права не должно содержать пробелы");
        }

        String normalizedName = ValidationUtils.normalizeString(name).toUpperCase(Locale.ROOT);
        String normalizedResource = ValidationUtils.normalizeString(resource).toLowerCase(Locale.ROOT);
        String normalizedDescription = ValidationUtils.normalizeString(description);

        this.name = normalizedName;
        this.resource = normalizedResource;
        this.description = normalizedDescription;
    }


    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {

        if (namePattern == null) namePattern = "";
        if (resourcePattern == null) resourcePattern = "";

        String normalizedNamePattern = namePattern.toUpperCase(Locale.ROOT);
        String normalizedResourcePattern = resourcePattern.toLowerCase(Locale.ROOT);

        boolean nameMatches = normalizedNamePattern.isEmpty() ||
                this.name.contains(normalizedNamePattern) ||
                this.name.matches(normalizedNamePattern);

        boolean resourceMatches = normalizedResourcePattern.isEmpty() ||
                this.resource.contains(normalizedResourcePattern) ||
                this.resource.matches(normalizedResourcePattern);

        return nameMatches && resourceMatches;
    }
}