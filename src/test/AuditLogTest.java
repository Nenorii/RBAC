import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void logAddsEntries() {
        AuditLog log = new AuditLog();

        log.log("2026-03-09 10:00:00", "USER_CREATE", "admin", "john", "Создание пользователя");
        log.log("2026-03-09 10:01:00", "ROLE_CREATE", "admin", "Manager", "Создание роли");

        List<AuditLog.AuditEntry> all = log.getAll();
        assertEquals(2, all.size());
        assertEquals("USER_CREATE", all.get(0).action());
        assertEquals("ROLE_CREATE", all.get(1).action());
    }

    @Test
    void getByPerformerFiltersCorrectly() {
        AuditLog log = new AuditLog();

        log.log("2026-03-09 10:00:00", "USER_CREATE", "admin", "john", "Создание пользователя");
        log.log("2026-03-09 10:01:00", "ROLE_CREATE", "manager", "Manager", "Создание роли");
        log.log("2026-03-09 10:02:00", "USER_DELETE", "admin", "john", "Удаление пользователя");

        List<AuditLog.AuditEntry> adminEntries = log.getByPerformer("admin");
        assertEquals(2, adminEntries.size());
        assertTrue(adminEntries.stream().allMatch(e -> "admin".equals(e.performer())));

        List<AuditLog.AuditEntry> managerEntries = log.getByPerformer("manager");
        assertEquals(1, managerEntries.size());
        assertEquals("ROLE_CREATE", managerEntries.get(0).action());
    }

    @Test
    void getByActionFiltersCorrectly() {
        AuditLog log = new AuditLog();

        log.log("2026-03-09 10:00:00", "USER_CREATE", "admin", "john", "Создание пользователя");
        log.log("2026-03-09 10:01:00", "USER_CREATE", "manager", "jane", "Создание пользователя");
        log.log("2026-03-09 10:02:00", "ROLE_CREATE", "admin", "Manager", "Создание роли");

        List<AuditLog.AuditEntry> userCreates = log.getByAction("USER_CREATE");
        assertEquals(2, userCreates.size());
        assertTrue(userCreates.stream().allMatch(e -> "USER_CREATE".equals(e.action())));
    }

    @Test
    void printLogDoesNotThrowOnEmptyAndNonEmpty() {
        AuditLog log = new AuditLog();

        assertDoesNotThrow(log::printLog);

        log.log("2026-03-09 10:00:00", "USER_CREATE", "admin", "john", "Создание пользователя");
        assertDoesNotThrow(log::printLog);
    }

    @Test
    void saveToFileCreatesFileWithEntries() throws IOException {
        AuditLog log = new AuditLog();
        log.log("2026-03-09 10:00:00", "USER_CREATE", "admin", "john", "Создание пользователя");

        Path temp = Files.createTempFile("audit-log-test", ".txt");
        try {
            log.saveToFile(temp.toString());

            assertTrue(Files.exists(temp));
            String content = Files.readString(temp);
            assertTrue(content.contains("USER_CREATE"));
            assertTrue(content.contains("admin"));
            assertTrue(content.contains("john"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
