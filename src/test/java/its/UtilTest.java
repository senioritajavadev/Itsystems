package its;

import its.util.Formats;
import its.util.Phone;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilTest {

    @Test
    void phoneVariantsNormalizeToSameNumber() {
        Optional<String> expected = Optional.of("79991234567");
        assertEquals(expected, Phone.normalize("+7 (999) 123-45-67"));
        assertEquals(expected, Phone.normalize("8 999 123 45 67"));
        assertEquals(expected, Phone.normalize("9991234567"));
        assertEquals(expected, Phone.normalize("79991234567"));
    }

    @Test
    void incompletePhoneIsRejected() {
        assertEquals(Optional.empty(), Phone.normalize("+7 999 123"));
        assertEquals(Optional.empty(), Phone.normalize(""));
        assertEquals(Optional.empty(), Phone.normalize(null));
    }

    @Test
    void phoneIsFormattedForDisplay() {
        assertEquals("+7 (999) 123-45-67", Phone.format("79991234567"));
        assertEquals("+375291234567", Phone.format("375291234567"));
        assertEquals("", Phone.format(""));
    }

    @Test
    void dateParsing() {
        assertEquals(LocalDate.of(2026, 11, 13), Formats.parseDate(" 13.11.2026 ", "Дата"));
        assertNull(Formats.parseDate("  ", "Дата"));
        assertThrows(IllegalArgumentException.class, () -> Formats.parseDate("31.02.2026", "Дата"));
        assertThrows(IllegalArgumentException.class, () -> Formats.parseDate("2026-11-13", "Дата"));
        assertEquals("13.11.2026", Formats.date(LocalDate.of(2026, 11, 13)));
    }

    @Test
    void moneyParsingAndFormatting() {
        assertEquals(new BigDecimal("1500.50"), Formats.parseMoney("1 500,5", "Цена"));
        assertEquals(new BigDecimal("800.00"), Formats.parseMoney("800", "Цена"));
        assertThrows(IllegalArgumentException.class, () -> Formats.parseMoney("-5", "Цена"));
        assertThrows(IllegalArgumentException.class, () -> Formats.parseMoney("abc", "Цена"));
        assertEquals("12 345,60 ₽", Formats.money(new BigDecimal("12345.6")));
    }
}
