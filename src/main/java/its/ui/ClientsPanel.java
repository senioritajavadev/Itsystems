package its.ui;

import its.dao.ClientDao;
import its.model.ClientRow;
import its.util.Phone;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/** Клиентская база. */
final class ClientsPanel extends JPanel implements Refreshable {
    private final ClientDao clients;
    private final DefaultTableModel model = Ui.readOnlyModel("ФИО", "Мобильный", "Заказов");
    private final JTable table = Ui.table(model, this::editSelected);
    private final JTextField search;
    private List<ClientRow> rows = List.of();

    ClientsPanel(ClientDao clients) {
        super(new BorderLayout(0, 14));
        this.clients = clients;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        search = Ui.searchField("Поиск: ФИО или телефон", this::refresh);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        toolbar.add(Ui.primaryButton("+ Клиент", () -> openForm(null)));
        toolbar.add(Ui.button("Изменить", this::editSelected));
        toolbar.add(Ui.button("Удалить", this::deleteSelected));
        toolbar.add(search);

        Ui.alignRight(table, 2);
        table.getColumnModel().getColumn(0).setPreferredWidth(360);

        add(toolbar, BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            rows = clients.findAll(search.getText());
            model.setRowCount(0);
            for (ClientRow row : rows) {
                model.addRow(new Object[]{
                        row.client().fullName(), Phone.format(row.client().phone()), row.ordersCount()});
            }
        });
    }

    private ClientRow selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private void editSelected() {
        ClientRow row = selected();
        if (row != null) {
            openForm(row);
        }
    }

    private void openForm(ClientRow existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.client().fullName());
        JTextField phone = new JTextField(existing == null ? "" : Phone.format(existing.client().phone()));
        JPanel form = new Ui.Form().add("ФИО", name).add("Мобильный", phone).build();
        form.setBorder(null);
        form.setPreferredSize(new Dimension(420, form.getPreferredSize().height));

        // Форма остаётся открытой, пока данные не сохранятся или пользователь не отменит ввод
        while (JOptionPane.showConfirmDialog(this, form, existing == null ? "Новый клиент" : "Клиент",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                String fullName = name.getText().strip();
                if (fullName.isEmpty()) {
                    throw new IllegalArgumentException("ФИО: заполните поле");
                }
                String digits = Phone.normalize(phone.getText()).orElseThrow(() -> new IllegalArgumentException(
                        "Мобильный: введите номер полностью, например +7 999 123-45-67"));
                if (existing == null) {
                    clients.create(fullName, digits);
                } else {
                    clients.update(existing.client().id(), fullName, digits);
                }
                refresh();
                return;
            } catch (RuntimeException e) {
                Ui.error(this, e);
            }
        }
    }

    private void deleteSelected() {
        ClientRow row = selected();
        if (row != null && Ui.confirm(this, "Удалить клиента «" + row.client().fullName() + "» из базы?")) {
            Ui.guard(this, () -> clients.delete(row.client().id()));
            refresh();
        }
    }
}
