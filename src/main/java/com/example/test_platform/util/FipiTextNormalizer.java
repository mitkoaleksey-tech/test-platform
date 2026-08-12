package com.example.test_platform.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FipiTextNormalizer {

    private static final Pattern UNICODE_VECTOR_PATTERN = Pattern.compile("→\\s*(\\S)");
    private static final Pattern FIPI_FRACTION_POWER_PATTERN1 = Pattern.compile("\\(\\s*1\\s+7\\s*\\)\\s*(\\S+)");
    private static final Pattern FIPI_FRACTION_POWER_PATTERN2 = Pattern.compile("\\(\\s*1\\s*/\\s*7\\s*\\)\\s*(\\S+)");

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        // 0. Очистка псевдо-математических HTML-таблиц ФИПИ
        String cleaned = cleanPseudoMathTables(text);

        // 1. Очистка невидимых символов Unicode, тире и кавычек
        cleaned = cleaned.replaceAll("[\\u2061\\u2062\\u2063\\u2064\\u200b\\u200c\\u200d\\u200e\\u200f\\ufeff]", "")
                .replace('\u2009', ' ')
                .replace('\u200a', ' ')
                .replace('\u202f', ' ')
                .replace('\u00a0', ' ')
                .replace(" ", " ")
                .replace(" ", " ")
                .replace("𝑎", "a")
                .replace("𝑏", "b")
                .replace("𝑐", "c")
                .replace("𝑥", "x")
                .replace("𝑦", "y")
                .replace("−", "-")
                .replace('\u2212', '-');

        // 2. Удаление артефактов заглушек рисунков: [рис.], [рис. 1], (см. рис.), [рисунок 1]
        cleaned = cleaned.replaceAll("(?i)\\[\\s*рис\\.?\\s*\\d*\\s*\\]", "")
                         .replaceAll("(?i)\\(\\s*см\\.?\\s*рис\\.?\\s*\\d*\\s*\\)", "")
                         .replaceAll("(?i)\\[\\s*рисунок\\s*\\d*\\s*\\]", "");

        // 3. Форматирование подпунктов вопросов: а), б), в), г) переносом на новую строку
        cleaned = cleaned.replaceAll("(?<=^|[\\.\\!\\?\\;\\s])([а-гА-Г]\\))\\s*", "\n$1 ");

        // 4. Форматирование текстовых таблиц с пайпами |
        cleaned = cleaned.replaceAll("(\\s+\\d+\\s*\\|)", "\n$1");

        // 5. Векторы и дроби ФИПИ
        Matcher vectorMatcher = UNICODE_VECTOR_PATTERN.matcher(cleaned);
        StringBuffer vectorBuffer = new StringBuffer();
        while (vectorMatcher.find()) {
            String letter = vectorMatcher.group(1);
            if (letter.equals("-") || letter.equals("+")) {
                continue;
            }
            vectorMatcher.appendReplacement(vectorBuffer, Matcher.quoteReplacement("$\\vec{" + letter + "}$"));
        }
        vectorMatcher.appendTail(vectorBuffer);
        cleaned = vectorBuffer.toString();

        Matcher fracMatcher1 = FIPI_FRACTION_POWER_PATTERN1.matcher(cleaned);
        StringBuffer fracBuffer1 = new StringBuffer();
        while (fracMatcher1.find()) {
            String power = fracMatcher1.group(1).trim();
            fracMatcher1.appendReplacement(fracBuffer1, Matcher.quoteReplacement("$\\left(\\frac{1}{7}\\right)^{" + power + "}$"));
        }
        fracMatcher1.appendTail(fracBuffer1);
        cleaned = fracBuffer1.toString();

        Matcher fracMatcher2 = FIPI_FRACTION_POWER_PATTERN2.matcher(cleaned);
        StringBuffer fracBuffer2 = new StringBuffer();
        while (fracMatcher2.find()) {
            String power = fracMatcher2.group(1).trim();
            fracMatcher2.appendReplacement(fracBuffer2, Matcher.quoteReplacement("$\\left(\\frac{1}{7}\\right)^{" + power + "}$"));
        }
        fracMatcher2.appendTail(fracBuffer2);
        cleaned = fracBuffer2.toString();

        // Clean up double math delimiters
        cleaned = cleaned.replace("$$", "$");

        // Нормализация пробелов и переносов строк
        cleaned = cleaned.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n");

        // Преобразование текстовых таблиц с пайпами | в стильные HTML-таблицы <table>
        return convertPipesToHtmlTable(cleaned.trim());
    }

    public static String convertPipesToHtmlTable(String text) {
        if (text == null || !text.contains("|")) {
            return text;
        }

        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean inTable = false;
        boolean isFirstRow = true;
        int i = 0;

        while (i < lines.length) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                if (inTable) {
                    boolean hasNextPipe = false;
                    for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                        String next = lines[j].trim();
                        if (next.equals("|") || next.contains("|") || next.endsWith(":") || next.toLowerCase().contains("пакеты")) {
                            hasNextPipe = true;
                            break;
                        }
                    }
                    if (hasNextPipe) {
                        i++;
                        continue;
                    } else {
                        sb.append("</table>\n\n");
                        inTable = false;
                        isFirstRow = true;
                    }
                }
                i++;
                continue;
            }

            // Случай 1: Многострочная последовательность с пайпами: Ячейка 1 \n | \n Ячейка 2 (\n | \n Ячейка 3 ...)
            if (i + 2 < lines.length && lines[i + 1].trim().equals("|") && !line.isEmpty() && !line.equals("|")) {
                java.util.List<String> rowCells = new java.util.ArrayList<>();
                rowCells.add(line);
                int curr = i + 1;
                while (curr < lines.length && lines[curr].trim().equals("|")) {
                    if (curr + 1 < lines.length && !lines[curr + 1].trim().isEmpty() && !lines[curr + 1].trim().equals("|")) {
                        rowCells.add(lines[curr + 1].trim());
                        curr += 2;
                    } else {
                        curr += 1;
                    }
                }

                if (rowCells.size() >= 2) {
                    if (!inTable) {
                        inTable = true;
                        isFirstRow = true;
                        sb.append("<table class=\"fipi-table\">\n");
                    }

                    String tag = isFirstRow ? "th" : "td";
                    sb.append("  <tr>");
                    for (String cell : rowCells) {
                        sb.append("<").append(tag).append(">").append(cell).append("</").append(tag).append(">");
                    }
                    sb.append("</tr>\n");
                    isFirstRow = false;
                    i = curr;
                    continue;
                }
            }

            // Случай 2: Заголовок внутри таблицы (например "В абонентскую плату включены пакеты:")
            if (inTable && (line.endsWith(":") || line.toLowerCase().contains("пакеты") || line.toLowerCase().contains("расходования"))) {
                sb.append("  <tr><td colspan=\"4\" style=\"font-weight:bold; background:var(--bg-hover, #f1f5f9); text-align:center; padding: 6px;\">").append(line).append("</td></tr>\n");
                i++;
                continue;
            }

            // Случай 3: Однострочная таблица "Cell1 | Cell2 | Cell3"
            if (line.contains("|")) {
                String[] cells = line.split("\\|");
                java.util.List<String> validCells = new java.util.ArrayList<>();
                for (String c : cells) {
                    if (!c.trim().isEmpty()) validCells.add(c.trim());
                }
                if (validCells.size() >= 2) {
                    if (!inTable) {
                        inTable = true;
                        isFirstRow = true;
                        sb.append("<table class=\"fipi-table\">\n");
                    }
                    String tag = isFirstRow ? "th" : "td";
                    sb.append("  <tr>");
                    for (String c : validCells) {
                        sb.append("<").append(tag).append(">").append(c).append("</").append(tag).append(">");
                    }
                    sb.append("</tr>\n");
                    isFirstRow = false;
                    i++;
                    continue;
                } else if (validCells.size() == 1) {
                    if (!inTable) {
                        inTable = true;
                        isFirstRow = true;
                        sb.append("<table class=\"fipi-table\">\n");
                    }
                    sb.append("  <tr><td colspan=\"4\" style=\"font-weight:bold; text-align:center;\">").append(validCells.get(0)).append("</td></tr>\n");
                    i++;
                    continue;
                }
            }

            if (inTable) {
                sb.append("</table>\n\n");
                inTable = false;
                isFirstRow = true;
            }

            sb.append(lines[i]).append("\n");
            i++;
        }

        if (inTable) {
            sb.append("</table>\n");
        }

        return sb.toString().trim();
    }

    public static String formatForPdf(String text) {
        if (text == null) return "";
        String normalized = normalize(text);

        String pdfText = normalized
                .replaceAll("\\$\\$", "")
                .replaceAll("\\$", "")
                .replaceAll("\\\\(?:d)?frac\\{([^\\}]+)\\}\\s*\\{([^\\}]+)\\}", "($1 / $2)")
                .replaceAll("\\\\sqrt\\{([^\\}]+)\\}", "√($1)")
                .replaceAll("\\\\vec\\{([^\\}]+)\\}", "вектор $1")
                .replaceAll("\\\\sin\\(([^\\)]+)\\)", "sin($1)")
                .replaceAll("\\\\cos\\(([^\\)]+)\\)", "cos($1)")
                .replaceAll("\\\\tan\\(([^\\)]+)\\)", "tg($1)")
                .replaceAll("\\\\cot\\(([^\\)]+)\\)", "ctg($1)")
                .replaceAll("\\\\pi", "π")
                .replaceAll("\\\\cdot", "·")
                .replaceAll("\\\\times", "×")
                .replaceAll("\\\\pm", "±")
                .replaceAll("\\\\leq", "≤")
                .replaceAll("\\\\geq", "≥")
                .replaceAll("\\\\neq", "≠")
                .replaceAll("\\\\left", "")
                .replaceAll("\\\\right", "")
                .replaceAll("\\\\\\{", "{")
                .replaceAll("\\\\\\}", "}")
                .replaceAll("\\^\\{([^\\}]+)\\}", "^($1)")
                .replaceAll("\\\\[a-zA-Z]+", "");

        return pdfText.trim();
    }

    public static String cleanPseudoMathTables(String text) {
        if (text == null || !text.contains("<table")) {
            return text;
        }

        Pattern mathTablePattern = Pattern.compile("<table[^>]*>\\s*<tr>\\s*(?:<th[^>]*>.*?</th>|<td[^>]*>.*?</td>)+\\s*</tr>\\s*</table>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = mathTablePattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tableContent = matcher.group(0);
            if (tableContent.contains("$") || tableContent.contains("\\frac") || tableContent.contains("^2") || tableContent.contains("x-a^2") || tableContent.contains("fipi-table")) {
                String cleanCells = tableContent.replaceAll("</?(?:table|tr|th|td)[^>]*>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                cleanCells = cleanCells.replace("$(", "$(").replace(")$", ")$");
                if (!cleanCells.startsWith("$") && !cleanCells.startsWith("$$")) {
                    cleanCells = "$" + cleanCells + "$";
                }
                cleanCells = cleanCells.replace("$$$", "$$").replace("$$", "$");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(cleanCells));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(tableContent));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
