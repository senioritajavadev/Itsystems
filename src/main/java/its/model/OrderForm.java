package its.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Данные формы заказа. id == null — новый заказ. Телефон уже нормализован. price == null — цена не указана. */
public record OrderForm(
        Integer id,
        LocalDate receivedDate,
        LocalDate issuedDate,
        String deviceBrand,
        String deviceModel,
        String clientName,
        String clientPhone,
        String defect,
        String afterWhat,
        String notes,
        BigDecimal price
) {
}
