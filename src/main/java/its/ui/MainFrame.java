package its.ui;

import com.formdev.flatlaf.FlatClientProperties;
import its.dao.AnalyticsDao;
import its.dao.ClientDao;
import its.dao.ExpenseDao;
import its.dao.OrderDao;
import its.dao.SaleDao;
import its.dao.ServiceDao;
import its.db.Database;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Главное окно: логотип и шесть вкладок. Открывается на Главной. */
public final class MainFrame extends JFrame {
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(Database db) {
        super("IT-S");
        ClientDao clients = new ClientDao(db);
        OrderDao orders = new OrderDao(db);
        ServiceDao services = new ServiceDao(db);
        SaleDao sales = new SaleDao(db);

        HomePanel home = new HomePanel(orders, clients);
        tabs.addTab("Главная", home);
        tabs.addTab("Продажи", new SalesPanel(sales, clients, services));
        tabs.addTab("Расходы", new ExpensesPanel(new ExpenseDao(db)));
        tabs.addTab("Финансовая аналитика", new AnalyticsPanel(new AnalyticsDao(db)));
        tabs.addTab("Клиентская база", new ClientsPanel(clients));
        tabs.addTab("Услуги и цены", new ServicesPanel(services));
        tabs.putClientProperty(FlatClientProperties.STYLE, "tabInsets: 4,18,4,18; font: +1");

        BufferedImage logo = loadLogo();
        if (logo != null) {
            setIconImage(logo);
            // У логотипа широкие чёрные поля — в шапку берём только сам знак с надписью
            Image mark = logo.getSubimage(15, 195, 610, 215).getScaledInstance(-1, 30, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(mark));
            label.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 14));
            tabs.putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, label);
        }

        // Каждая вкладка перечитывает данные при открытии: правки на одной сразу видны на других
        tabs.addChangeListener(e -> refreshSelected());
        tabs.setSelectedIndex(0);
        home.refresh();

        setContentPane(tabs);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    public void selectTab(int index) {
        tabs.setSelectedIndex(index);
    }

    private void refreshSelected() {
        if (tabs.getSelectedComponent() instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private static BufferedImage loadLogo() {
        try (InputStream in = MainFrame.class.getResourceAsStream("/its/logo.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    public static void installTheme() {
        Ui.installTheme();
    }
}
