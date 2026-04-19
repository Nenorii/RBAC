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
            System.out.println(FormatUtils.formatHeader("RBAC Консоль v3.0 (с асинхронными задачами)"));
            System.out.println("Все команды: 'help'");
            System.out.println("Новые команды: 'report-users-async', 'save-async', 'workers-status'");
            System.out.println("Выход: 'exit'");
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