package its.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class Formats {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** Пустая строка — null. Иначе дата в формате ДД.ММ.ГГГГ. */
    public static LocalDate parseDate(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip(), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + ": введите дату в формате ДД.ММ.ГГГГ");
        }
    }

    public static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(amount) + " ₽";
    }

    /** Принимает «1500», «1 500,50», «1500.5». */
    public static BigDecimal parseMoney(String text, String fieldName) {
        String cleaned = text == null ? "" : text.replaceAll("[\s\u00A0₽]", "").replace(',', '.');
        try {
            BigDecimal value = new BigDecimal(cleaned).setScale(2, java.math.RoundingMode.HALF_UP);
            if (value.signum() < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + ": введите неотрицательное число, например 1500,50");
        }
    }
}
