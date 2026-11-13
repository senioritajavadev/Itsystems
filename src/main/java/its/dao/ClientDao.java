package its.dao;

import its.db.DataException;
import its.db.Database;
import its.model.Client;
import its.model.ClientRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientDao {
    private static final String DUPLICATE = "Клиент с таким номером уже есть в базе";
    private final Database db;

    public ClientDao(Database db) {
        this.db = db;
    }

    /** Поиск клиента по нормализованному номеру — для автоподстановки в формах. */
    public Optional<Client> findByPhone(String phone) {
        return db.query("SELECT id, full_name, phone FROM its.clients WHERE phone = ?",
                rs -> new Client(rs.getInt("id"), rs.getString("full_name"), rs.getString("phone")),
                phone).stream().findFirst();
    }

    public List<ClientRow> findAll(String search) {
        return db.query("""
                SELECT c.id, c.full_name, c.phone, COUNT(o.id) AS orders_count
                FROM its.clients c
                LEFT JOIN its.orders o ON o.client_id = c.id
                WHERE (c.full_name || ' ' || c.phone) ILIKE ?
                GROUP BY c.id
                ORDER BY c.full_name, c.id
                """,
                rs -> new ClientRow(
                        new Client(rs.getInt("id"), rs.getString("full_name"), rs.getString("phone")),
                        rs.getInt("orders_count")),
                Daos.like(search));
    }

    public void create(String fullName, String phone) {
        try {
            db.update("INSERT INTO its.clients (full_name, phone) VALUES (?, ?)", fullName, phone);
        } catch (DataException e) {
            throw Daos.friendly(e, DUPLICATE);
        }
    }

    public void update(int id, String fullName, String phone) {
        try {
            db.update("UPDATE its.clients SET full_name = ?, phone = ? WHERE id = ?", fullName, phone, id);
        } catch (DataException e) {
            throw Daos.friendly(e, DUPLICATE);
        }
    }

    public void delete(int id) {
        try {
            db.update("DELETE FROM its.clients WHERE id = ?", id);
        } catch (DataException e) {
            throw Daos.friendly(e, "Нельзя удалить клиента, у которого есть заказы");
        }
    }

    /**
     * Возвращает id клиента с этим номером; если его нет — создаёт.
     * Если клиент есть, но ФИО введено другое, ФИО обновляется.
     */
    static int findOrCreate(Connection c, String fullName, String phone) throws SQLException {
        return Database.query(c, """
                INSERT INTO its.clients (full_name, phone) VALUES (?, ?)
                ON CONFLICT (phone) DO UPDATE SET full_name = EXCLUDED.full_name
                RETURNING id
                """, rs -> rs.getInt(1), fullName, phone).get(0);
    }
}
