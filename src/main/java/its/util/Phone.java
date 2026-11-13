package its.util;

import java.util.Optional;

/** Телефон хранится в базе только цифрами, российские номера — в виде 7XXXXXXXXXX. */
public final class Phone {
    private Phone() {
    }

    /** Возвращает нормализованный номер или пусто, если введённое на номер не похоже. */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 10) {
            digits = "7" + digits;
        } else if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        return digits.length() >= 11 && digits.length() <= 15 ? Optional.of(digits) : Optional.empty();
    }

    public static String format(String digits) {
        if (digits == null || digits.isEmpty()) {
            return "";
        }
        if (digits.length() == 11 && digits.startsWith("7")) {
            return "+7 (%s) %s-%s-%s".formatted(
                    digits.substring(1, 4), digits.substring(4, 7), digits.substring(7, 9), digits.substring(9));
        }
        return "+" + digits;
    }
}
