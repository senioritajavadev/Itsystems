package its.ui;

import its.dao.ExpenseDao;
import its.model.Expense;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Расходы: журнал трат сервисного центра. Учитываются в финансовой аналитике. */
final class ExpensesPanel extends JPanel implements Refreshable {
    private final ExpenseDao expenses;
    private final DefaultTableModel model = Ui.readOnlyModel("Дата", "Статья расхода", "Сумма");
    private final JTable table = Ui.table(model, this::editSelected);
    private final JTextField search;
    private final JLabel total = new JLabel();
    private List<Expense> rows = List.of();

    ExpensesPanel(ExpenseDao expenses) {
        super(new BorderLayout(0, 14));
        this.expenses = expenses;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        search = Ui.searchField("Поиск по статье расхода", this::refresh);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        toolbar.add(Ui.primaryButton("+ Расход", () -> openForm(null)));
        toolbar.add(Ui.button("Изменить", this::editSelected));
        toolbar.add(Ui.button("Удалить", this::deleteSelected));
        toolbar.add(search);

        Ui.alignRight(table, 2);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        total.setFont(total.getFont().deriveFont(Font.BOLD, total.getFont().getSize2D() + 2f));

        add(toolbar, BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
        add(total, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            rows = expenses.findAll(search.getText());
            model.setRowCount(0);
            BigDecimal sum = BigDecimal.ZERO;
            for (Expense expense : rows) {
                model.addRow(new Object[]{
                        Formats.date(expense.date()), expense.title(), Formats.money(expense.amount())});
                sum = sum.add(expense.amount());
            }
            total.setText("Расходов: " + rows.size() + "    Итого: " + Formats.money(sum));
        });
    }

    private Expense selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private void editSelected() {
        Expense expense = selected();
        if (expense != null) {
            openForm(expense);
        }
    }

    private void openForm(Expense existing) {
        JTextField date = new JTextField(Formats.date(existing == null ? LocalDate.now() : existing.date()));
        JTextField title = new JTextField(existing == null ? "" : existing.title());
        JTextField amount = new JTextField(
                existing == null ? "" : existing.amount().toPlainString().replace('.', ','));
        JPanel form = new Ui.Form()
                .add("Дата", date)
                .add("Статья расхода", title)
                .add("Сумма, ₽", amount)
                .build();
        form.setBorder(null);
        form.setPreferredSize(new Dimension(420, form.getPreferredSize().height));

        // Форма остаётся открытой, пока данные не сохранятся или пользователь не отменит ввод
        while (JOptionPane.showConfirmDialog(this, form, existing == null ? "Новый расход" : "Расход",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                LocalDate day = Formats.parseDate(date.getText(), "Дата");
                if (day == null) {
                    throw new IllegalArgumentException("Дата: заполните поле");
                }
                String name = title.getText().strip();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Статья расхода: заполните поле");
                }
                BigDecimal sum = Formats.parseMoney(amount.getText(), "Сумма");
                expenses.save(existing == null ? null : existing.id(), day, name, sum);
                refresh();
                return;
            } catch (RuntimeException e) {
                Ui.error(this, e);
            }
        }
    }

    private void deleteSelected() {
        Expense expense = selected();
        if (expense != null && Ui.confirm(this, "Удалить расход «" + expense.title() + "» от "
                + Formats.date(expense.date()) + "?")) {
            Ui.guard(this, () -> expenses.delete(expense.id()));
            refresh();
        }
    }
}
