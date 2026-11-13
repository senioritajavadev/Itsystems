package its.dao;

import its.db.DataException;
import its.db.Database;
import its.model.ServiceItem;

import java.math.BigDecimal;
import java.util.List;

public class ServiceDao {
    private static final String DUPLICATE = "Услуга с таким названием уже есть";
    private final Database db;

    public ServiceDao(Database db) {
        this.db = db;
    }

    public List<ServiceItem> findAll(String search) {
        return db.query("SELECT id, name, price FROM its.services WHERE name ILIKE ? ORDER BY name",
                rs -> new ServiceItem(rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price")),
                Daos.like(search));
    }

    public void create(String name, BigDecimal price) {
        try {
            db.update("INSERT INTO its.services (name, price) VALUES (?, ?)", name, price);
        } catch (DataException e) {
            throw Daos.friendly(e, DUPLICATE);
        }
    }

    public void update(int id, String name, BigDecimal price) {
        try {
            db.update("UPDATE its.services SET name = ?, price = ? WHERE id = ?", name, price, id);
        } catch (DataException e) {
            throw Daos.friendly(e, DUPLICATE);
        }
    }

    public void delete(int id) {
        db.update("DELETE FROM its.services WHERE id = ?", id);
    }
}
