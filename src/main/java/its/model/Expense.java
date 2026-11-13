package its.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Расход сервисного центра: аренда, запчасти, зарплата и т. п. */
public record Expense(int id, LocalDate date, String title, BigDecimal amount) {
}
