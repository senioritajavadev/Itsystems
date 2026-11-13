package its.ui;

import its.dao.ClientDao;
import its.dao.SaleDao;
import its.model.Sale;
import its.model.ServiceItem;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Форма продажи. Наименование можно выбрать из прайса (цена подставится) или ввести вручную. */
final class SaleDialog extends JDialog {
    private final SaleDao sales;
    private final Sale existing;
    private final List<ServiceItem> services;

    private final JTextField date = new JTextField();
    private final JComboBox<String> title = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100_000, 1));
    private final JTextField price = new JTextField();
    private final ClientFields client;
    private boolean saved;

    private SaleDialog(Window owner, SaleDao sales, ClientDao clients, List<ServiceItem> services, Sale existing) {
        super(owner, existing == null ? "Новая продажа" : "Продажа", ModalityType.APPLICATION_MODAL);
        this.sales = sales;
        this.existing = existing;
        this.services = services;
        this.client = new ClientFields(clients, false);

        title.setEditable(true);
        services.forEach(s -> title.addItem(s.name()));
        title.setSelectedItem(existing == null ? "" : existing.title());
        date.setText(Formats.date(existing == null ? LocalDate.now() : existing.date()));
        if (existing != null) {
            quantity.setValue(existing.quantity());
            price.setText(existing.price().toPlainString().replace('.', ','));
            client.set(existing.clientPhone(), existing.clientName());
        }
        // Слушатель вешаем после заполнения, чтобы не затереть цену редактируемой продажи
        title.addActionListener(e -> fillPriceFromServices());

        JPanel form = new Ui.Form()
                .add("Дата", date)
                .add("Наименование", title)
                .add("Количество", quantity)
                .add("Цена за единицу, ₽", price)
                .add("Мобильный (необязательно)", client.phone)
                .hint(client.hint)
                .add("ФИО клиента", client.name)
                .build();

        JButton save = Ui.primaryButton("Сохранить", this::save);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));
        buttons.add(Ui.button("Отмена", this::dispose));
        buttons.add(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        pack();
        setSize(new Dimension(Math.max(getWidth(), 520), getHeight()));
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    static boolean show(Component parent, SaleDao sales, ClientDao clients, List<ServiceItem> services,
                        Sale existing) {
        SaleDialog dialog = new SaleDialog(SwingUtilities.getWindowAncestor(parent), sales, clients, services,
                existing);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private String titleText() {
        return String.valueOf(title.getEditor().getItem()).strip();
    }

    private void fillPriceFromServices() {
        String name = titleText();
        services.stream()
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(s -> price.setText(s.price().toPlainString().replace('.', ',')));
    }

    private void save() {
        Ui.guard(this, () -> {
            LocalDate saleDate = Formats.parseDate(date.getText(), "Дата");
            if (saleDate == null) {
                throw new IllegalArgumentException("Дата: заполните поле");
            }
            String name = titleText();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Наименование: заполните поле");
            }
            BigDecimal unitPrice = Formats.parseMoney(price.getText(), "Цена");
            sales.save(existing == null ? null : existing.id(), saleDate, name, (Integer) quantity.getValue(),
                    unitPrice, client.nameValue(), client.phoneValue());
            saved = true;
            dispose();
        });
    }
}
