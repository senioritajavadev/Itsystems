package its.model;

/** Строка вкладки «Клиентская база»: клиент и число его заказов. */
public record ClientRow(Client client, int ordersCount) {
}
