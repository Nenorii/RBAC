import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nЗавершение работы системы...");
            system.shutdown();
            System.out.println("Система завершена.");
        }));

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println(FormatUtils.formatHeader("RBAC Консоль"));
            System.out.println("Все команды: 'help'");
            System.out.println("Асинхронные команды: 'report-users-async', 'save-async', 'workers-status'");
            System.out.println("Команды планировщика: 'scheduler-status', 'check-expired-now'");
            System.out.println("");
            System.out.println("Периодические задачи:");
            System.out.println("  - Проверка истекших ролей: каждые 10 секунд");
            System.out.println("  - Отчёт о статистике: каждые 30 секунд");
            System.out.println("");
            System.out.println(system.generateStatistics());
            System.out.println();

            while (true) {
                System.out.print(FormatUtils.padRight("> ", 4));
                String input = scanner.nextLine();
                parser.parseAndExecute(input, scanner, system);
            }
        }
    }
}