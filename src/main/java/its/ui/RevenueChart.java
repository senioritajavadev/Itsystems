package its.ui;

import its.model.Analytics;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Столбчатая диаграмма выручки и расходов за последние 12 месяцев. */
final class RevenueChart extends JPanel {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("LLL yy", Locale.forLanguageTag("ru"));
    private static final DateTimeFormatter MONTH_FULL =
            DateTimeFormatter.ofPattern("LLLL uuuu", Locale.forLanguageTag("ru"));
    private static final Color GRID = new Color(0x27303B);

    private List<Analytics.MonthRevenue> months = List.of();
    /** Области месяцев на графике — для всплывающих подсказок. */
    private final List<java.awt.Rectangle> slots = new ArrayList<>();

    RevenueChart() {
        setBackground(Ui.CARD);
        setBorder(BorderFactory.createLineBorder(Ui.CARD_BORDER));
        setPreferredSize(new Dimension(0, 220));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    void setData(List<Analytics.MonthRevenue> months) {
        this.months = months;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (int i = 0; i < slots.size(); i++) {
            java.awt.Rectangle slot = slots.get(i);
            if (event.getX() >= slot.x && event.getX() < slot.x + slot.width) {
                Analytics.MonthRevenue m = months.get(i);
                return MONTH_FULL.format(m.month()) + ": выручка " + Formats.money(m.revenue())
                        + "  ·  расходы " + Formats.money(m.expenses())
                        + "  ·  прибыль " + Formats.money(m.profit());
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        slots.clear();
        if (months.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();

        double max = months.stream().map(m -> m.revenue().max(m.expenses()))
                .mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double top = niceCeiling(max);

        int left = 16 + fm.stringWidth(shortMoney(top));
        int right = getWidth() - 20;
        int topY = 42;

        int legendX = legend(g, fm, "Расходы", Ui.EXPENSE, right, 8);
        legend(g, fm, "Выручка", Ui.ACCENT, legendX - 16, 8);
        int bottomY = getHeight() - 16 - fm.getHeight();
        int plotHeight = bottomY - topY;

        // Сетка: 4 горизонтальные линии с подписями
        for (int i = 0; i <= 4; i++) {
            int y = bottomY - plotHeight * i / 4;
            g.setColor(GRID);
            g.drawLine(left, y, right, y);
            g.setColor(Ui.MUTED);
            String label = shortMoney(top * i / 4);
            g.drawString(label, left - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }

        double slot = (right - left) / (double) months.size();
        // В каждом месяце два столбика рядом: выручка и расходы
        int barWidth = (int) Math.min(24, slot * 0.32);
        for (int i = 0; i < months.size(); i++) {
            Analytics.MonthRevenue m = months.get(i);
            double revenue = m.revenue().doubleValue();
            int center = (int) (left + slot * i + slot / 2);
            slots.add(new java.awt.Rectangle((int) (left + slot * i), topY, (int) Math.ceil(slot), plotHeight));

            int revenueTop = drawBar(g, Ui.ACCENT, center - barWidth - 1, barWidth, revenue, top, bottomY,
                    plotHeight);
            int expensesTop = drawBar(g, Ui.EXPENSE, center + 1, barWidth, m.expenses().doubleValue(), top,
                    bottomY, plotHeight);
            g.setColor(GRID);
            g.drawLine(center - barWidth - 1, bottomY, center + 1 + barWidth, bottomY);

            // Подписывается только выручка — цветом своего столбика; расходы видны в подсказке
            if (revenue > 0) {
                String valueLabel = shortMoney(revenue);
                g.setColor(Ui.ACCENT);
                g.drawString(valueLabel, center - fm.stringWidth(valueLabel) / 2,
                        Math.min(revenueTop, expensesTop) - 5);
            }
            String monthLabel = MONTH.format(m.month()).replace(".", "");
            g.setColor(Ui.MUTED);
            g.drawString(monthLabel, center - fm.stringWidth(monthLabel) / 2, bottomY + fm.getAscent() + 6);
        }
        g.dispose();
    }

    /** Рисует столбик со скруглённым верхом и возвращает координату его верхней границы. */
    private int drawBar(Graphics2D g, Color color, int x, int width, double value, double top, int bottomY,
                        int plotHeight) {
        int height = top == 0 ? 0 : (int) Math.round(plotHeight * value / top);
        if (value > 0) {
            height = Math.max(height, 2);
        }
        if (height > 0) {
            g.setColor(color);
            g.fillRoundRect(x, bottomY - height, width, height + 4, 6, 6);
            g.setColor(getBackground());
            g.fillRect(x, bottomY, width, 5);
        }
        return bottomY - height;
    }

    /** Рисует пункт легенды, прижатый правым краем к xRight; возвращает его левый край. */
    private static int legend(Graphics2D g, FontMetrics fm, String text, Color color, int xRight, int y) {
        int textX = xRight - fm.stringWidth(text);
        g.setColor(Ui.MUTED);
        g.drawString(text, textX, y + fm.getAscent());
        int box = 10;
        int boxX = textX - 6 - box;
        g.setColor(color);
        g.fillRoundRect(boxX, y + (fm.getHeight() - box) / 2, box, box, 3, 3);
        return boxX;
    }

    /** Округляет максимум вверх до «красивого» числа, чтобы подписи сетки были ровными. */
    private static double niceCeiling(double max) {
        if (max <= 0) {
            return 1000;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        for (double step : new double[]{1, 2, 4, 5, 8, 10}) {
            if (max <= step * magnitude) {
                return step * magnitude;
            }
        }
        return 10 * magnitude;
    }

    private static String shortMoney(double value) {
        if (value >= 1_000_000) {
            return trim(value / 1_000_000) + " млн";
        }
        if (value >= 1_000) {
            return trim(value / 1_000) + " тыс";
        }
        return trim(value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text.replace('.', ',');
    }
}
