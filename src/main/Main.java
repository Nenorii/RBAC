import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println(FormatUtils.formatHeader("RBAC Консоль v2.0"));
            System.out.println("Все команды: 'help'");
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
