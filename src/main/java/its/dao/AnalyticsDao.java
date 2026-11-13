package its.dao;

import its.db.Database;
import its.model.Analytics;
import its.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AnalyticsDao {
    /**
     * Вся выручка одной таблицей: продажи плюс выполненные заказы с указанной ценой.
     * Заказ учитывается по дате выдачи; пока он «В работе», в выручку не входит.
     */
    private static final String REVENUE = """
            WITH revenue AS (
                SELECT sale_date AS day, title, quantity, quantity * price AS total
                FROM its.sales
                UNION ALL
                SELECT COALESCE(issued_date, received_date),
                       CASE WHEN btrim(device_brand || ' ' || device_model) = '' THEN 'Ремонт'
                            ELSE 'Ремонт: ' || btrim(device_brand || ' ' || device_model) END,
                       1,
                       price
                FROM its.orders
                WHERE status = 'DONE' AND price IS NOT NULL
            )
            """;

    private final Database db;

    public AnalyticsDao(Database db) {
        this.db = db;
    }

    public Analytics load() {
        record Totals(BigDecimal today, BigDecimal month, BigDecimal year, BigDecimal total, int count) {
        }
        Totals totals = db.query(REVENUE + """
                SELECT COALESCE(SUM(total) FILTER (WHERE day = CURRENT_DATE), 0) AS today,
                       COALESCE(SUM(total) FILTER (
                           WHERE date_trunc('month', day) = date_trunc('month', CURRENT_DATE)), 0) AS month,
                       COALESCE(SUM(total) FILTER (
                           WHERE date_trunc('year', day) = date_trunc('year', CURRENT_DATE)), 0) AS year,
                       COALESCE(SUM(total), 0) AS total,
                       COUNT(*) AS cnt
                FROM revenue
                """,
                rs -> new Totals(rs.getBigDecimal("today"), rs.getBigDecimal("month"),
                        rs.getBigDecimal("year"), rs.getBigDecimal("total"), rs.getInt("cnt"))).get(0);

        record Expenses(BigDecimal month, BigDecimal year, BigDecimal total) {
        }
        Expenses expenses = db.query("""
                SELECT COALESCE(SUM(amount) FILTER (
                           WHERE date_trunc('month', expense_date) = date_trunc('month', CURRENT_DATE)), 0) AS month,
                       COALESCE(SUM(amount) FILTER (
                           WHERE date_trunc('year', expense_date) = date_trunc('year', CURRENT_DATE)), 0) AS year,
                       COALESCE(SUM(amount), 0) AS total
                FROM its.expenses
                """,
                rs -> new Expenses(rs.getBigDecimal("month"), rs.getBigDecimal("year"), rs.getBigDecimal("total")))
                .get(0);

        // Подзапросы вместо двух JOIN: иначе строки выручки и расходов одного месяца перемножились бы
        List<Analytics.MonthRevenue> months = db.query(REVENUE + """
                SELECT m::date AS month,
                       (SELECT COALESCE(SUM(r.total), 0) FROM revenue r
                        WHERE date_trunc('month', r.day::timestamp) = m) AS revenue,
                       (SELECT COALESCE(SUM(e.amount), 0) FROM its.expenses e
                        WHERE date_trunc('month', e.expense_date::timestamp) = m) AS expenses
                FROM generate_series(date_trunc('month', CURRENT_DATE::timestamp) - interval '11 months',
                                     date_trunc('month', CURRENT_DATE::timestamp),
                                     interval '1 month') AS m
                ORDER BY m
                """,
                rs -> new Analytics.MonthRevenue(rs.getObject("month", LocalDate.class),
                        rs.getBigDecimal("revenue"), rs.getBigDecimal("expenses")));

        List<Analytics.TopItem> top = db.query(REVENUE + """
                SELECT title, SUM(quantity) AS qty, SUM(total) AS revenue
                FROM revenue
                GROUP BY title
                ORDER BY revenue DESC, title
                LIMIT 5
                """,
                rs -> new Analytics.TopItem(rs.getString("title"), rs.getInt("qty"), rs.getBigDecimal("revenue")));

        return new Analytics(totals.today(), totals.month(), totals.year(), totals.total(),
                expenses.month(), expenses.year(), expenses.total(), totals.count(),
                countOrders(OrderStatus.IN_PROGRESS), countOrders(OrderStatus.DONE), months, top);
    }

    private int countOrders(OrderStatus status) {
        return db.query("SELECT COUNT(*) FROM its.orders WHERE status = ?", rs -> rs.getInt(1), status.name())
                .get(0);
    }
}
