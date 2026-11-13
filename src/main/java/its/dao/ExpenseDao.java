package its.dao;

import its.db.Database;
import its.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExpenseDao {
    private final Database db;

    public ExpenseDao(Database db) {
        this.db = db;
    }

    public List<Expense> findAll(String search) {
        return db.query("""
                SELECT id, expense_date, title, amount
                FROM its.expenses
                WHERE title ILIKE ?
                ORDER BY expense_date DESC, id DESC
                """,
                rs -> new Expense(
                        rs.getInt("id"),
                        rs.getObject("expense_date", LocalDate.class),
                        rs.getString("title"),
                        rs.getBigDecimal("amount")),
                Daos.like(search));
    }

    /** Создаёт (id == null) или обновляет расход. */
    public void save(Integer id, LocalDate date, String title, BigDecimal amount) {
        if (id == null) {
            db.update("INSERT INTO its.expenses (expense_date, title, amount) VALUES (?, ?, ?)",
                    date, title, amount);
        } else {
            db.update("UPDATE its.expenses SET expense_date = ?, title = ?, amount = ? WHERE id = ?",
                    date, title, amount, id);
        }
    }

    public void delete(int id) {
        db.update("DELETE FROM its.expenses WHERE id = ?", id);
    }
}
