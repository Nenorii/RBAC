import java.util.List;
import java.util.Scanner;

public final class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + ": ");
            String line = scanner.nextLine();
            if (line != null) {
                line = line.trim();
            }
            if (!required || (line != null && !line.isEmpty())) {
                return line;
            }
            System.out.println("Поле обязательно для заполнения. Попробуйте ещё раз.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " (" + min + "–" + max + "): ");
            String raw = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    System.out.println("Число должно быть в диапазоне от " + min + " до " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Нужно ввести число. Попробуйте ещё раз.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (да/нет): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("да") || ans.equals("y") || ans.equals("yes")) {
                return true;
            }
            if (ans.equals("нет") || ans.equals("n") || ans.equals("no")) {
                return false;
            }
            System.out.println("Введите 'да' или 'нет'.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов пуст");
        }
        while (true) {
            System.out.println(message + ":");
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ". " + options.get(i));
            }
            System.out.print("Выберите номер: ");
            String raw = scanner.nextLine().trim();
            try {
                int idx = Integer.parseInt(raw) - 1;
                if (idx < 0 || idx >= options.size()) {
                    System.out.println("Неверный номер. Попробуйте ещё раз.");
                    continue;
                }
                return options.get(idx);
            } catch (NumberFormatException e) {
                System.out.println("Нужно ввести номер варианта. Попробуйте ещё раз.");
            }
        }
    }
}
