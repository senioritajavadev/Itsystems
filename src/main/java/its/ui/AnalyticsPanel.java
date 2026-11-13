package its.ui;

import its.dao.AnalyticsDao;
import its.model.Analytics;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** Финансовая аналитика: выручка, расходы и прибыль, динамика по месяцам, самые доходные позиции. */
final class AnalyticsPanel extends JPanel implements Refreshable {
    private final AnalyticsDao analytics;
    private final JLabel today = new JLabel();
    private final JLabel month = new JLabel();
    private final JLabel year = new JLabel();
    private final JLabel total = new JLabel();
    private final JLabel average = new JLabel();
    private final JLabel orders = new JLabel();
    private final JLabel expensesMonth = new JLabel();
    private final JLabel expensesYear = new JLabel();
    private final JLabel expensesTotal = new JLabel();
    private final JLabel profitMonth = new JLabel();
    private final JLabel profitYear = new JLabel();
    private final JLabel profitTotal = new JLabel();
    private final RevenueChart chart = new RevenueChart();
    private final DefaultTableModel topModel = Ui.readOnlyModel("Позиция", "Продано, шт.", "Выручка");

    AnalyticsPanel(AnalyticsDao analytics) {
        super(new BorderLayout(0, 16));
        this.analytics = analytics;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel tiles = new JPanel(new GridLayout(2, 6, 12, 12));
        tiles.add(tile("Выручка сегодня", today));
        tiles.add(tile("Выручка за месяц", month));
        tiles.add(tile("Выручка за год", year));
        tiles.add(tile("Выручка за всё время", total));
        tiles.add(tile("Средний чек", average));
        tiles.add(tile("В работе / выполнено", orders));
        tiles.add(tile("Расходы за месяц", expensesMonth));
        tiles.add(tile("Расходы за год", expensesYear));
        tiles.add(tile("Расходы за всё время", expensesTotal));
        tiles.add(tile("Прибыль за месяц", profitMonth));
        tiles.add(tile("Прибыль за год", profitYear));
        tiles.add(tile("Прибыль за всё время", profitTotal));

        JPanel chartBox = section("Выручка и расходы по месяцам, ₽", chart);

        JTable top = Ui.table(topModel, () -> { });
        top.setRowSelectionAllowed(false);
        top.setFocusable(false);
        Ui.alignRight(top, 1, 2);
        top.getColumnModel().getColumn(0).setPreferredWidth(420);
        JPanel topBox = section("Самые доходные позиции", Ui.scroll(top));
        topBox.setPreferredSize(new Dimension(0, 236));

        add(tiles, BorderLayout.NORTH);
        add(chartBox, BorderLayout.CENTER);
        add(topBox, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            Analytics data = analytics.load();
            today.setText(Formats.money(data.revenueToday()));
            month.setText(Formats.money(data.revenueMonth()));
            year.setText(Formats.money(data.revenueYear()));
            total.setText(Formats.money(data.revenueTotal()));
            average.setText(Formats.money(data.averageCheck()));
            orders.setText(data.ordersInProgress() + " / " + data.ordersDone());
            expensesMonth.setText(Formats.money(data.expensesMonth()));
            expensesYear.setText(Formats.money(data.expensesYear()));
            expensesTotal.setText(Formats.money(data.expensesTotal()));
            showProfit(profitMonth, data.profitMonth());
            showProfit(profitYear, data.profitYear());
            showProfit(profitTotal, data.profitTotal());
            chart.setData(data.months());
            topModel.setRowCount(0);
            for (Analytics.TopItem item : data.topItems()) {
                topModel.addRow(new Object[]{item.title(), item.quantity(), Formats.money(item.revenue())});
            }
        });
    }

    /** Убыток выделяется красным. */
    private static void showProfit(JLabel label, BigDecimal profit) {
        label.setText(Formats.money(profit));
        label.setForeground(profit.signum() < 0 ? Ui.DANGER : UIManager.getColor("Label.foreground"));
    }

    private static JPanel tile(String caption, JLabel value) {
        value.setFont(value.getFont().deriveFont(Font.BOLD, value.getFont().getSize2D() + 5f));
        JPanel tile = new JPanel(new BorderLayout(0, 6));
        tile.setBackground(Ui.CARD);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.CARD_BORDER),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        tile.add(Ui.muted(caption), BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    private static JPanel section(String caption, java.awt.Component content) {
        JLabel title = new JLabel(caption);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2f));
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.add(title, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        return section;
    }
}
