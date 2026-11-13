package its.ui;

import its.dao.ClientDao;
import its.dao.OrderDao;
import its.model.OrderStatus;
import its.model.RepairOrder;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Главная: заказы «В работе»; выполненные скрыты и показываются второй колонкой по галочке. */
final class HomePanel extends JPanel implements Refreshable, OrderCard.Actions {
    private final OrderDao orders;
    private final ClientDao clients;
    private final JTextField search;
    private final Column inProgress = new Column(OrderStatus.IN_PROGRESS);
    private final Column done = new Column(OrderStatus.DONE);
    private final Set<Integer> expanded = new HashSet<>();
    private final JCheckBox showDone = new JCheckBox("Показать выполненные");
    private final JPanel columns = new JPanel();

    HomePanel(OrderDao orders, ClientDao clients) {
        super(new BorderLayout(0, 14));
        this.orders = orders;
        this.clients = clients;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        search = Ui.searchField("Поиск: номер, устройство, клиент, телефон", this::refresh);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        toolbar.add(Ui.primaryButton("+ Новый заказ", this::createOrder));
        toolbar.add(search);
        toolbar.add(showDone);
        showDone.addActionListener(e -> {
            layoutColumns();
            refresh();
        });

        layoutColumns();
        add(toolbar, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
    }

    /** Колонка «Выполнено» по умолчанию скрыта и появляется только по галочке. */
    private void layoutColumns() {
        columns.removeAll();
        columns.setLayout(new GridLayout(1, showDone.isSelected() ? 2 : 1, 20, 0));
        columns.add(inProgress);
        if (showDone.isSelected()) {
            columns.add(done);
        }
        columns.revalidate();
        columns.repaint();
    }

    @Override
    public void refresh() {
        Ui.guard(this, () -> {
            inProgress.show(orders.findByStatus(OrderStatus.IN_PROGRESS, search.getText()));
            if (showDone.isSelected()) {
                done.show(orders.findByStatus(OrderStatus.DONE, search.getText()));
            }
        });
    }

    private void createOrder() {
        if (OrderDialog.show(this, orders, clients, null)) {
            refresh();
        }
    }

    @Override
    public void toggled(RepairOrder order, boolean isExpanded) {
        if (isExpanded) {
            expanded.add(order.id());
        } else {
            expanded.remove(order.id());
        }
    }

    @Override
    public void edit(RepairOrder order) {
        if (OrderDialog.show(this, orders, clients, order)) {
            refresh();
        }
    }

    @Override
    public void changeStatus(RepairOrder order) {
        Ui.guard(this, () -> orders.setStatus(order.id(),
                order.status() == OrderStatus.IN_PROGRESS ? OrderStatus.DONE : OrderStatus.IN_PROGRESS));
        refresh();
    }

    @Override
    public void delete(RepairOrder order) {
        if (Ui.confirm(this, "Удалить заказ № " + order.id() + "? Вернуть его будет нельзя.")) {
            Ui.guard(this, () -> orders.delete(order.id()));
            expanded.remove(order.id());
            refresh();
        }
    }

    private final class Column extends JPanel {
        private final OrderStatus status;
        private final JLabel title = new JLabel();
        private final CardList cards = new CardList();

        Column(OrderStatus status) {
            super(new BorderLayout(0, 10));
            this.status = status;
            title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 4f));
            JScrollPane scroll = new JScrollPane(cards);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(title, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
        }

        void show(List<RepairOrder> list) {
            title.setText(status.title() + "  ·  " + list.size());
            cards.removeAll();
            if (list.isEmpty()) {
                JLabel empty = Ui.muted(search.getText().isBlank() ? "Заказов пока нет" : "Ничего не найдено");
                empty.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
                cards.add(empty);
            }
            for (RepairOrder order : list) {
                cards.add(new OrderCard(order, expanded.contains(order.id()), HomePanel.this));
                cards.add(Box.createVerticalStrut(10));
            }
            cards.revalidate();
            cards.repaint();
        }
    }

    /** Вертикальный список карточек, который растягивается по ширине области прокрутки. */
    private static final class CardList extends JPanel implements Scrollable {
        CardList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
