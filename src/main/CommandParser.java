import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

final class CommandParser {

    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    void registerCommand(String name, String description, Command command) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Имя команды не может быть пустым");
        if (command == null)
            throw new IllegalArgumentException("Command не может быть null");
        commands.put(name, command);
        commandDescriptions.put(name, description != null ? description : "");
    }

    void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command cmd = commands.get(commandName);
        if (cmd == null) {
            System.out.println("Неизвестная команда: " + commandName);
            System.out.println("Введите 'help' для списка доступных команд.");
            return;
        }
        cmd.execute(scanner, system);
    }

    void printHelp() {
        System.out.println("Доступные команды:");
        commands.keySet().stream().sorted().forEach(name -> {
            String desc = commandDescriptions.getOrDefault(name, "");
            System.out.printf("  %-20s %s%n", name, desc);
        });
    }

    void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }
        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        executeCommand(commandName, scanner, system);
    }
}
