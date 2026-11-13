package its.dao;

import its.db.DataException;
import its.db.Database;
import its.model.Sale;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SaleDao {
    private final Database db;

    public SaleDao(Database db) {
        this.db = db;
    }

    public List<Sale> findAll(String search) {
        return db.query("""
                SELECT s.id, s.sale_date, s.title, s.quantity, s.price,
                       COALESCE(c.full_name, '') AS full_name, COALESCE(c.phone, '') AS phone
                FROM its.sales s
                LEFT JOIN its.clients c ON c.id = s.client_id
                WHERE (s.title || ' ' || COALESCE(c.full_name, '') || ' ' || COALESCE(c.phone, '')) ILIKE ?
                ORDER BY s.sale_date DESC, s.id DESC
                """,
                rs -> new Sale(
                        rs.getInt("id"),
                        rs.getObject("sale_date", LocalDate.class),
                        rs.getString("title"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getString("full_name"),
                        rs.getString("phone")),
                Daos.like(search));
    }

    /**
     * Создаёт (id == null) или обновляет продажу. Клиент необязателен: если clientPhone == null,
     * продажа сохраняется без клиента, иначе клиент находится по телефону или заводится в базе.
     */
    public void save(Integer id, LocalDate date, String title, int quantity, BigDecimal price,
                     String clientName, String clientPhone) {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                Integer clientId = clientPhone == null ? null : ClientDao.findOrCreate(c, clientName, clientPhone);
                if (id == null) {
                    Database.update(c, """
                            INSERT INTO its.sales (sale_date, title, quantity, price, client_id)
                            VALUES (?, ?, ?, ?, ?)
                            """, date, title, quantity, price, clientId);
                } else {
                    Database.update(c, """
                            UPDATE its.sales
                            SET sale_date = ?, title = ?, quantity = ?, price = ?, client_id = ?
                            WHERE id = ?
                            """, date, title, quantity, price, clientId, id);
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataException("Не удалось сохранить продажу", e);
        }
    }

    public void delete(int id) {
        db.update("DELETE FROM its.sales WHERE id = ?", id);
    }
}
