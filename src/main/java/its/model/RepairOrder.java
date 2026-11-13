package its.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Карточка заказа на Главной. */
public record RepairOrder(
        int id,
        LocalDate receivedDate,
        LocalDate issuedDate,
        String deviceBrand,
        String deviceModel,
        String clientName,
        String clientPhone,
        String defect,
        String afterWhat,
        String notes,
        BigDecimal price,
        OrderStatus status
) {
    public String device() {
        String device = (deviceBrand + " " + deviceModel).strip();
        return device.isEmpty() ? "Устройство не указано" : device;
    }
}
