import java.time.LocalDateTime;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        return new AssignmentMetadata(assignedBy, LocalDateTime.now().toString(), reason);
    }

    public static AssignmentMetadata now(String assignedBy) {
        return now(assignedBy, null);
    }

    public String format() {
        if (reason == null || reason.isBlank()) {
            return String.format("Assigned by: %s at %s", assignedBy, assignedAt);
        }
        return String.format("Assigned by: %s at %s\nReason: %s",
                assignedBy, assignedAt, reason);
    }
}