import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt = "";

    public TemporaryAssignment(User user, Role role, AssignmentMetadata assignmentMetadata, String date) {
        super(user, role, assignmentMetadata);
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата окончания обязательна для временного назначения");
        }
        if (!DateUtils.isFutureDate(date)) {
            throw new IllegalArgumentException("Дата окончания должна быть в будущем");
        }
        this.expiresAt = date;
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Укажите корректную новую дату!");
        }
        if (!DateUtils.isFutureDate(newExpirationDate)) {
            throw new IllegalArgumentException("Новая дата должна быть в будущем");
        }
        this.expiresAt = newExpirationDate;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return DateUtils.isExpired(expiresAt);
    }

    public String getTimeRemaining() {
        if (expiresAt.isEmpty()) {
            return "Never expires";
        }

        try {
            LocalDateTime expirationDate = DateUtils.parseExpiry(expiresAt);
            LocalDateTime now = LocalDateTime.now();

            if (expirationDate.isBefore(now)) {
                return "Истёкло";
            }

            long days = DateUtils.daysUntil(expiresAt);
            if (days > 0) {
                return days + " дней осталось";
            }

            Duration duration = Duration.between(now, expirationDate);
            long hours = duration.toHoursPart();
            long minutes = duration.toMinutesPart();

            if (hours > 0) {
                return String.format("%d ч %d мин", hours, minutes);
            } else {
                return String.format("%d мин", minutes);
            }

        } catch (DateTimeParseException e) {
            return "Неверный формат даты";
        }
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();  // базовая часть из родителя

        return baseSummary + String.format(" [TEMPORARY, до %s, %s]",
                DateUtils.format(expiresAt),
                isActive() ? "активно" : "истекло");
    }
}
