import java.util.List;
import java.util.Arrays;

public final class FormatUtils {

    private FormatUtils() {}

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers.length == 0 || rows.isEmpty()) {
            return "Пустая таблица";
        }

        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = Math.max(headers[i].length(), widths[i]);
        }
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                widths[i] = Math.max(truncate(row[i], 100).length(), widths[i]);
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("+").append(hLine(widths)).append("+\n");

        sb.append("|");
        for (int i = 0; i < headers.length; i++) {
            sb.append(padRight(headers[i], widths[i])).append("|");
        }
        sb.append("\n");

        sb.append("+").append(hLine(widths)).append("+\n");

        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < headers.length; i++) {
                String cell = i < row.length ? truncate(row[i], widths[i]) : "";
                sb.append(padRight(cell, widths[i])).append("|");
            }
            sb.append("\n");
        }

        sb.append("+").append(hLine(widths)).append("+\n");

        return sb.toString();
    }

    private static String hLine(int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int width : widths) {
            line.append("-".repeat(width + 2));
        }
        return line.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "+---+\n|   |\n+---+";
        }

        String[] lines = text.split("\n");
        int maxWidth = Arrays.stream(lines)
                .mapToInt(String::length)
                .max()
                .orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append("+" + "-".repeat(maxWidth + 2) + "+\n");

        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxWidth)).append(" |\n");
        }

        sb.append("+" + "-".repeat(maxWidth + 2) + "+\n");
        return sb.toString();
    }

    public static String formatHeader(String text) {
        String border = "=".repeat(Math.max(10, text.length() + 4));
        return String.format("+ %s +\n| %s |\n+ %s +\n",
                border, padRight(text, border.length() - 4), border);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        return String.format("%-" + length + "s", text);
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        return String.format("%" + length + "s", text);
    }
}
