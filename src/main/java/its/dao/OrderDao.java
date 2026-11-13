package its.dao;

import its.db.DataException;
import its.db.Database;
import its.model.OrderForm;
import its.model.OrderStatus;
import its.model.RepairOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class OrderDao {
    private final Database db;

    public OrderDao(Database db) {
        this.db = db;
    }

    public List<RepairOrder> findByStatus(OrderStatus status, String search) {
        return db.query("""
                SELECT o.id, o.received_date, o.issued_date, o.device_brand, o.device_model,
                       c.full_name, c.phone, o.defect, o.after_what, o.notes, o.price, o.status
                FROM its.orders o
                JOIN its.clients c ON c.id = o.client_id
                WHERE o.status = ?
                  AND (o.id || ' ' || o.device_brand || ' ' || o.device_model || ' '
                       || c.full_name || ' ' || c.phone) ILIKE ?
                ORDER BY o.id DESC
                """,
                rs -> new RepairOrder(
                        rs.getInt("id"),
                        rs.getObject("received_date", LocalDate.class),
                        rs.getObject("issued_date", LocalDate.class),
                        rs.getString("device_brand"),
                        rs.getString("device_model"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("defect"),
                        rs.getString("after_what"),
                        rs.getString("notes"),
                        rs.getBigDecimal("price"),
                        OrderStatus.valueOf(rs.getString("status"))),
                status.name(), Daos.like(search));
    }

    /** Создаёт или обновляет заказ; клиент находится по телефону или заводится в базе. Возвращает номер заказа. */
    public int save(OrderForm form) {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                int clientId = ClientDao.findOrCreate(c, form.clientName(), form.clientPhone());
                int orderId;
                if (form.id() == null) {
                    orderId = Database.query(c, """
                            INSERT INTO its.orders (received_date, issued_date, device_brand, device_model,
                                                    client_id, defect, after_what, notes, price)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            RETURNING id
                            """, rs -> rs.getInt(1),
                            form.receivedDate(), form.issuedDate(), form.deviceBrand(), form.deviceModel(),
                            clientId, form.defect(), form.afterWhat(), form.notes(), form.price()).get(0);
                } else {
                    orderId = form.id();
                    Database.update(c, """
                            UPDATE its.orders
                            SET received_date = ?, issued_date = ?, device_brand = ?, device_model = ?,
                                client_id = ?, defect = ?, after_what = ?, notes = ?, price = ?
                            WHERE id = ?
                            """,
                            form.receivedDate(), form.issuedDate(), form.deviceBrand(), form.deviceModel(),
                            clientId, form.defect(), form.afterWhat(), form.notes(), form.price(), orderId);
                }
                c.commit();
                return orderId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataException("Не удалось сохранить заказ", e);
        }
    }

    /**
     * Перенос между «В работе» и «Выполнено». При выполнении проставляется дата выдачи (если её не было),
     * при возврате в работу — очищается.
     */
    public void setStatus(int orderId, OrderStatus status) {
        if (status == OrderStatus.DONE) {
            db.update("""
                    UPDATE its.orders
                    SET status = 'DONE', issued_date = COALESCE(issued_date, CURRENT_DATE)
                    WHERE id = ?
                    """, orderId);
        } else {
            db.update("UPDATE its.orders SET status = 'IN_PROGRESS', issued_date = NULL WHERE id = ?", orderId);
        }
    }

    public void delete(int orderId) {
        db.update("DELETE FROM its.orders WHERE id = ?", orderId);
    }
}
