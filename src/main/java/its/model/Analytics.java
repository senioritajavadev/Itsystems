package its.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Analytics(
        BigDecimal revenueToday,
        BigDecimal revenueMonth,
        BigDecimal revenueYear,
        BigDecimal revenueTotal,
        BigDecimal expensesMonth,
        BigDecimal expensesYear,
        BigDecimal expensesTotal,
        int salesCount,
        int ordersInProgress,
        int ordersDone,
        List<MonthRevenue> months,
        List<TopItem> topItems
) {
    public BigDecimal averageCheck() {
        return salesCount == 0
                ? BigDecimal.ZERO
                : revenueTotal.divide(BigDecimal.valueOf(salesCount), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Прибыль = выручка минус расходы; может быть отрицательной. */
    public BigDecimal profitMonth() {
        return revenueMonth.subtract(expensesMonth);
    }

    public BigDecimal profitYear() {
        return revenueYear.subtract(expensesYear);
    }

    public BigDecimal profitTotal() {
        return revenueTotal.subtract(expensesTotal);
    }

    public record MonthRevenue(LocalDate month, BigDecimal revenue, BigDecimal expenses) {
        public BigDecimal profit() {
            return revenue.subtract(expenses);
        }
    }

    public record TopItem(String title, int quantity, BigDecimal revenue) {
    }
}
