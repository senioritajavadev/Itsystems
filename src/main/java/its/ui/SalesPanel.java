package its.ui;

import its.dao.ClientDao;
import its.dao.SaleDao;
import its.dao.ServiceDao;
import its.model.Sale;
import its.util.Formats;
import its.util.Phone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;

/** Продажи: журнал продаж услуг и товаров. */
final class SalesPanel extends JPanel implements Refreshable {
    private final SaleDao sales;
    private final ClientDao clients;
    private final ServiceDao services;
    private final DefaultTableModel model =
            Ui.readOnlyModel("Дата", "Наименование", "Кол-во", "Цена", "Сумма", "Клиент", "Мобильный");
    private final JTable table = Ui.table(model, this::editSelected);
    private final JTextField search;
    private final JLabel total = new JLabel();
    private List<Sale> rows = List.of();

    SalesPanel(SaleDao sales, ClientDao clients, ServiceDao services) {
        super(new BorderLayout(0, 14));
        this.sales = sales;
        this.clients = clients;
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        search = Ui.searchField("Поиск: наименование, клиент, телефон", this::refresh);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        toolbar.add(Ui.primaryButton("+ Продажа", () -> openDialog(null)));
        toolbar.add(Ui.button("Изменить", this::editSelected));
        toolbar.add(Ui.button("Удалить", this::deleteSelected));
        toolbar.add(search);

        Ui.alignRight(table, 2, 3, 4);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);
        total.setFont(total.getFont().deriveFont(Font.BOLD, total.getFont().getSize2D() + 2f));

        add(toolbar, BorderLayout.NORTH);
        add(Ui.scroll(table), BorderLayout.CENTER);
        add(total, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            rows = sales.findAll(search.getText());
            model.setRowCount(0);
            BigDecimal sum = BigDecimal.ZERO;
            for (Sale sale : rows) {
                model.addRow(new Object[]{
                        Formats.date(sale.date()), sale.title(), sale.quantity(), Formats.money(sale.price()),
                        Formats.money(sale.total()), sale.clientName(), Phone.format(sale.clientPhone())});
                sum = sum.add(sale.total());
            }
            total.setText("Продаж: " + rows.size() + "    Итого: " + Formats.money(sum));
        });
    }

    private Sale selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private void editSelected() {
        Sale sale = selected();
        if (sale != null) {
            openDialog(sale);
        }
    }

    private void openDialog(Sale sale) {
        Ui.guard(this, () -> {
            if (SaleDialog.show(this, sales, clients, services.findAll(""), sale)) {
                refresh();
            }
        });
    }

    private void deleteSelected() {
        Sale sale = selected();
        if (sale != null && Ui.confirm(this, "Удалить продажу «" + sale.title() + "» от "
                + Formats.date(sale.date()) + "?")) {
            Ui.guard(this, () -> sales.delete(sale.id()));
            refresh();
        }
    }
}
