import java.util.Locale;

public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Название права не может быть пустым");
        if (resource == null || resource.isBlank())
            throw new IllegalArgumentException("Ресурс не может быть пустым");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Описание не должно быть пустым");

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Название права не должно содержать пробелы");
        }

        this.name = name.toUpperCase(Locale.ROOT);
        this.resource = resource.toLowerCase(Locale.ROOT);
        this.description = description.trim();
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