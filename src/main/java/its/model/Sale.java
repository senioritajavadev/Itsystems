package its.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Продажа. Клиент необязателен: clientName и clientPhone пустые, если он не указан. */
public record Sale(
        int id,
        LocalDate date,
        String title,
        int quantity,
        BigDecimal price,
        String clientName,
        String clientPhone
) {
    public BigDecimal total() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
