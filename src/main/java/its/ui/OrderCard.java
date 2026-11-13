package its.ui;

import its.model.OrderStatus;
import its.model.RepairOrder;
import its.util.Formats;
import its.util.Phone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Карточка заказа на Главной: свёрнутая — заголовок, раскрытая — все поля и действия. */
final class OrderCard extends JPanel {
    interface Actions {
        void toggled(RepairOrder order, boolean expanded);

        void edit(RepairOrder order);

        void changeStatus(RepairOrder order);

        void delete(RepairOrder order);
    }

    private final JPanel details;
    private final JLabel chevron = new JLabel();

    OrderCard(RepairOrder order, boolean expanded, Actions actions) {
        super(new BorderLayout());
        setBackground(Ui.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.CARD_BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("№ " + order.id() + "   " + order.device());
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1.5f));
        JLabel subtitle = Ui.muted(order.clientName() + "  ·  " + Phone.format(order.clientPhone()));
        JPanel titles = new JPanel(new BorderLayout(0, 3));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        chevron.setForeground(Ui.MUTED);
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(titles, BorderLayout.CENTER);
        header.add(chevron, BorderLayout.EAST);

        details = buildDetails(order, actions);
        details.setVisible(expanded);
        updateChevron();

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                details.setVisible(!details.isVisible());
                updateChevron();
                revalidate();
                actions.toggled(order, details.isVisible());
            }
        });

        add(header, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
    }

    private void updateChevron() {
        chevron.setText(details.isVisible() ? "▲" : "▼");
    }

    private static JPanel buildDetails(RepairOrder order, Actions actions) {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int row = 0;
        row = addField(fields, row, "Номер заказа", String.valueOf(order.id()));
        row = addField(fields, row, "Дата приёма", Formats.date(order.receivedDate()));
        row = addField(fields, row, "Дата выдачи", Formats.date(order.issuedDate()));
        row = addField(fields, row, "Марка устройства", order.deviceBrand());
        row = addField(fields, row, "Модель устройства", order.deviceModel());
        row = addField(fields, row, "ФИО клиента", order.clientName());
        row = addField(fields, row, "Мобильный", Phone.format(order.clientPhone()));
        row = addField(fields, row, "Неисправность", order.defect());
        row = addField(fields, row, "После чего", order.afterWhat());
        row = addField(fields, row, "Пометки", order.notes());
        addField(fields, row, "Цена", order.price() == null ? "" : Formats.money(order.price()));

        boolean inProgress = order.status() == OrderStatus.IN_PROGRESS;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(12, -8, 0, 0));
        buttons.add(inProgress
                ? Ui.primaryButton("Выполнено", () -> actions.changeStatus(order))
                : Ui.button("Вернуть в работу", () -> actions.changeStatus(order)));
        buttons.add(Ui.button("Изменить", () -> actions.edit(order)));
        buttons.add(Ui.button("Удалить", () -> actions.delete(order)));

        JPanel details = new JPanel(new BorderLayout());
        details.setOpaque(false);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 0, 0, 0),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.CARD_BORDER),
                        BorderFactory.createEmptyBorder(8, 0, 0, 0))));
        details.add(fields, BorderLayout.CENTER);
        details.add(buttons, BorderLayout.SOUTH);
        return details;
    }

    private static int addField(JPanel panel, int row, String label, String value) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(4, 0, 4, 16);
        panel.add(Ui.muted(label), c);

        // JTextArea вместо JLabel: длинные «Неисправность» и «Пометки» переносятся по словам
        JTextArea text = new JTextArea(value == null || value.isBlank() ? "—" : value);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setBorder(null);
        text.setFont(panel.getFont());
        text.setColumns(1);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        panel.add(text, c);
        return row + 1;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
