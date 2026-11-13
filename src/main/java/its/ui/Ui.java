package its.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/** Общие мелочи интерфейса: тема, цвета, диалоги, заготовки компонентов. */
final class Ui {
    static final Color ACCENT = new Color(0x1EA7F0);
    static final Color MUTED = new Color(0x8B96A5);
    static final Color CARD = new Color(0x1C222B);
    static final Color CARD_BORDER = new Color(0x2B3440);
    static final Color SUCCESS = new Color(0x3FB97A);
    static final Color EXPENSE = new Color(0xF0883E);
    static final Color DANGER = new Color(0xF0645A);

    private Ui() {
    }

    static void installTheme() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", "#1ea7f0",
                "@background", "#12161c"));
        FlatDarkLaf.setup();
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.tabHeight", 46);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("OptionPane.yesButtonText", "Да");
        UIManager.put("OptionPane.noButtonText", "Нет");
        UIManager.put("OptionPane.cancelButtonText", "Отмена");
    }

    /** Выполняет действие; любую ошибку показывает диалогом, а не роняет приложение. */
    static void guard(Component parent, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            error(parent, e);
        }
    }

    static void error(Component parent, Throwable e) {
        String text = e.getMessage() == null ? e.toString() : e.getMessage();
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            text += "\n\n" + e.getCause().getMessage();
        }
        JOptionPane.showMessageDialog(parent, text, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component parent, String question) {
        return JOptionPane.showConfirmDialog(parent, question, "Подтверждение",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static JButton primaryButton(String text, Runnable onClick) {
        JButton button = button(text, onClick);
        button.putClientProperty(FlatClientProperties.STYLE,
                "background: #1ea7f0; foreground: #06121c; font: bold; borderWidth: 0; focusWidth: 1");
        return button;
    }

    static JButton button(String text, Runnable onClick) {
        JButton button = new JButton(text);
        button.addActionListener(e -> onClick.run());
        return button;
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    static JTextField searchField(String placeholder, Runnable onChange) {
        JTextField field = new JTextField(30);
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        onChange(field, onChange);
        return field;
    }

    static void onChange(JTextComponent field, Runnable listener) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        });
    }

    static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    /** Таблица только для чтения с выбором одной строки; двойной щелчок вызывает onOpen. */
    static JTable table(DefaultTableModel model, Runnable onOpen) {
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onOpen.run();
                }
            }
        });
        return table;
    }

    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static void alignRight(JTable table, int... columns) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int column : columns) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    static JScrollPane scroll(JComponent content) {
        JScrollPane pane = new JScrollPane(content);
        pane.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        pane.getVerticalScrollBar().setUnitIncrement(18);
        return pane;
    }

    /** Форма «подпись — поле» в два столбца. */
    static final class Form {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private int row;

        Form add(String label, JComponent field) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.gridx = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(9, 0, 4, 12);
            panel.add(muted(label), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(4, 0, 4, 0);
            panel.add(field, c);
            return this;
        }

        /** Подсказка под предыдущим полем. */
        Form hint(JComponent hint) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 2, 4, 0);
            panel.add(hint, c);
            return this;
        }

        JPanel build() {
            panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
            return panel;
        }
    }
}
