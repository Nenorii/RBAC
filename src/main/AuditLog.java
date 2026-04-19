import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final List<AuditEntry> storage = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread consumerThread;

    public AuditLog() {
        consumerThread = new Thread(this::consume, "audit-log-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void consume() {
        while (running.get()) {
            try {
                AuditEntry entry = queue.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    storage.add(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void log(String timestamp, String action, String performer, String target, String details) {
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        queue.offer(entry);
    }

    public void log(String action, String performer, String target, String details) {
        String ts = DateUtils.getCurrentDateTime();
        log(ts, action, performer, target, details);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(storage);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null) return List.of();
        return storage.stream()
                .filter(e -> performer.equals(e.performer()))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null) return List.of();
        return storage.stream()
                .filter(e -> action.equals(e.action()))
                .toList();
    }

    public void printLog() {
        if (storage.isEmpty()) {
            System.out.println("Аудит‑лог пуст.");
            return;
        }
        System.out.println("AUDIT LOG");
        for (AuditEntry e : storage) {
            System.out.printf(
                    "[%s] %-15s by %-15s target=%-20s | %s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(), e.details()
            );
        }
    }

    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (AuditEntry e : storage) {
                writer.write(String.format(
                        "%s\t%s\t%s\t%s\t%s%n",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details()
                ));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Ошибка при сохранении audit‑лога в файл: " + filename, ex);
        }
    }

    public void shutdown() {
        running.set(false);
        consumerThread.interrupt();
        try {
            consumerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getQueueSize() {
        return queue.size();
    }
}