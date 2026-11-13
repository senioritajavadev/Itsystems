package its.ui;

import its.dao.ServiceDao;
import its.model.ServiceItem;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;

/** Услуги и цены: прайс-лист сервисного центра. */
final class ServicesPanel extends JPanel implements Refreshable {
    private final ServiceDao services;
    private final DefaultTableModel model = Ui.readOnlyModel("Услуга", "Цена");
    private final JTable table = Ui.table(model, this::editSelected);
    private final JTextField search;
    private List<ServiceItem> rows = List.of();

    ServicesPanel(ServiceDao services) {
        super(new BorderLayout(0, 14));
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        search = Ui.searchField("Поиск по названию", this::refresh);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        toolbar.add(Ui.primaryButton("+ Услуга", () -> openForm(null)));
        toolbar.add(Ui.button("Изменить", this::editSelected));
        toolbar.add(Ui.button("Удалить", this::deleteSelected));
        toolbar.add(search);

        Ui.alignRight(table, 1);
        table.getColumnModel().getColumn(0).setPreferredWidth(520);

        add(toolbar, BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            rows = services.findAll(search.getText());
            model.setRowCount(0);
            for (ServiceItem item : rows) {
                model.addRow(new Object[]{item.name(), Formats.money(item.price())});
            }
        });
    }

    private ServiceItem selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private void editSelected() {
        ServiceItem item = selected();
        if (item != null) {
            openForm(item);
        }
    }

    private void openForm(ServiceItem existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.name());
        JTextField price = new JTextField(
                existing == null ? "" : existing.price().toPlainString().replace('.', ','));
        JPanel form = new Ui.Form().add("Услуга", name).add("Цена, ₽", price).build();
        form.setBorder(null);
        form.setPreferredSize(new Dimension(420, form.getPreferredSize().height));

        // Форма остаётся открытой, пока данные не сохранятся или пользователь не отменит ввод
        while (JOptionPane.showConfirmDialog(this, form, existing == null ? "Новая услуга" : "Услуга",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                String title = name.getText().strip();
                if (title.isEmpty()) {
                    throw new IllegalArgumentException("Услуга: заполните название");
                }
                BigDecimal amount = Formats.parseMoney(price.getText(), "Цена");
                if (existing == null) {
                    services.create(title, amount);
                } else {
                    services.update(existing.id(), title, amount);
                }
                refresh();
                return;
            } catch (RuntimeException e) {
                Ui.error(this, e);
            }
        }
    }

    private void deleteSelected() {
        ServiceItem item = selected();
        if (item != null && Ui.confirm(this, "Удалить услугу «" + item.name() + "» из прайса?")) {
            Ui.guard(this, () -> services.delete(item.id()));
            refresh();
        }
    }
}
