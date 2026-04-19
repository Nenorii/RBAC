import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public void log(String timestamp, String action, String performer, String target, String details) {
        entries.add(new AuditEntry(timestamp, action, performer, target, details));
    }

    public void log(String action, String performer, String target, String details) {
        String ts = DateUtils.getCurrentDateTime();
        log(ts, action, performer, target, details);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null) return List.of();
        return entries.stream()
                .filter(e -> performer.equals(e.performer()))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null) return List.of();
        return entries.stream()
                .filter(e -> action.equals(e.action()))
                .toList();
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Аудит‑лог пуст.");
            return;
        }
        System.out.println("AUDIT LOG");
        for (AuditEntry e : entries) {
            System.out.printf(
                    "[%s] %-15s by %-15s target=%-20s | %s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(), e.details()
            );
        }
    }

    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (AuditEntry e : entries) {
                writer.write(String.format(
                        "%s\t%s\t%s\t%s\t%s%n",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details()
                ));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Ошибка при сохранении audit‑лога в файл: " + filename, ex);
        }
    }
}