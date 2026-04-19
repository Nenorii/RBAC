import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ExpiredAssignmentsChecker {

    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final AtomicLong expiredCount = new AtomicLong(0);

    public ExpiredAssignmentsChecker(AssignmentManager assignmentManager, AuditLog auditLog) {
        this.assignmentManager = assignmentManager;
        this.auditLog = auditLog;
    }

    public int checkAndMarkExpired() {
        List<RoleAssignment> activeAssignments = assignmentManager.getActiveAssignments();

        List<TemporaryAssignment> expired = activeAssignments.stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .map(a -> (TemporaryAssignment) a)
                .filter(TemporaryAssignment::isExpired)
                .collect(Collectors.toList());

        if (expired.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (TemporaryAssignment ta : expired) {
            try {
                assignmentManager.revokeAssignment(ta.assignmentId());
                count++;

                auditLog.log(
                        "EXPIRED_ASSIGNMENT",
                        "scheduler",
                        ta.user().username(),
                        "Роль '" + ta.role().getName() + "' истекла"
                );
            } catch (Exception e) {
                System.err.println("Ошибка при отзыве истекшей роли: " + e.getMessage());
            }
        }

        if (count > 0) {
            expiredCount.addAndGet(count);
            System.out.println("[SCHEDULER] Отозвано истекших назначений: " + count);
        }

        return count;
    }

    public String generateStatisticsReport() {
        int totalAssignments = assignmentManager.count();
        int activeAssignments = assignmentManager.getActiveAssignments().size();
        int expiredAssignments = assignmentManager.getExpiredAssignments().size();

        return String.format(
                "[STATS] Назначений: всего=%d, активных=%d, истекших=%d, всего отозвано планировщиком=%d",
                totalAssignments, activeAssignments, expiredAssignments, expiredCount.get()
        );
    }

    public long getTotalExpiredProcessed() {
        return expiredCount.get();
    }
}