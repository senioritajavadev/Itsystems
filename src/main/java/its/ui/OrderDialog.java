package its.ui;

import its.dao.ClientDao;
import its.dao.OrderDao;
import its.model.OrderForm;
import its.model.RepairOrder;
import its.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Форма заказа: создание и редактирование карточки. */
final class OrderDialog extends JDialog {
    private final OrderDao orders;
    private final RepairOrder existing;

    private final JTextField number = new JTextField();
    private final JTextField received = new JTextField();
    private final JTextField issued = new JTextField();
    private final JTextField brand = new JTextField();
    private final JTextField model = new JTextField();
    private final ClientFields client;
    private final JTextArea defect = Ui.textArea(3);
    private final JTextArea afterWhat = Ui.textArea(2);
    private final JTextArea notes = Ui.textArea(3);
    private final JTextField price = new JTextField();
    private boolean saved;

    private OrderDialog(Window owner, OrderDao orders, ClientDao clients, RepairOrder existing) {
        super(owner, existing == null ? "Новый заказ" : "Заказ № " + existing.id(), ModalityType.APPLICATION_MODAL);
        this.orders = orders;
        this.existing = existing;
        this.client = new ClientFields(clients, true);

        number.setEditable(false);
        number.setText(existing == null ? "будет присвоен при сохранении" : String.valueOf(existing.id()));
        received.setText(Formats.date(existing == null ? LocalDate.now() : existing.receivedDate()));
        if (existing != null) {
            issued.setText(Formats.date(existing.issuedDate()));
            brand.setText(existing.deviceBrand());
            model.setText(existing.deviceModel());
            client.set(existing.clientPhone(), existing.clientName());
            defect.setText(existing.defect());
            afterWhat.setText(existing.afterWhat());
            notes.setText(existing.notes());
            if (existing.price() != null) {
                price.setText(existing.price().toPlainString().replace('.', ','));
            }
        }

        JPanel form = new Ui.Form()
                .add("Номер заказа", number)
                .add("Дата приёма", received)
                .add("Дата выдачи", issued)
                .add("Марка устройства", brand)
                .add("Модель устройства", model)
                .add("Мобильный", client.phone)
                .hint(client.hint)
                .add("ФИО клиента", client.name)
                .add("Неисправность", new JScrollPane(defect))
                .add("После чего", new JScrollPane(afterWhat))
                .add("Пометки", new JScrollPane(notes))
                .add("Цена, ₽", price)
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
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /** Показывает форму; возвращает true, если заказ сохранён. existing == null — новый заказ. */
    static boolean show(Component parent, OrderDao orders, ClientDao clients, RepairOrder existing) {
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(parent);
        OrderDialog dialog = new OrderDialog(owner, orders, clients, existing);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void save() {
        Ui.guard(this, () -> {
            LocalDate receivedDate = Formats.parseDate(received.getText(), "Дата приёма");
            if (receivedDate == null) {
                throw new IllegalArgumentException("Дата приёма: заполните поле");
            }
            LocalDate issuedDate = Formats.parseDate(issued.getText(), "Дата выдачи");
            if (issuedDate != null && issuedDate.isBefore(receivedDate)) {
                throw new IllegalArgumentException("Дата выдачи не может быть раньше даты приёма");
            }
            // Цену можно не указывать при приёме и внести позже, когда ремонт оценён
            BigDecimal orderPrice = price.getText().isBlank() ? null : Formats.parseMoney(price.getText(), "Цена");
            orders.save(new OrderForm(
                    existing == null ? null : existing.id(),
                    receivedDate,
                    issuedDate,
                    brand.getText().strip(),
                    model.getText().strip(),
                    client.nameValue(),
                    client.phoneValue(),
                    defect.getText().strip(),
                    afterWhat.getText().strip(),
                    notes.getText().strip(),
                    orderPrice));
            saved = true;
            dispose();
        });
    }
}
