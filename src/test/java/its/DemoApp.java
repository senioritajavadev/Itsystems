package its;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import its.dao.ExpenseDao;
import its.dao.OrderDao;
import its.dao.SaleDao;
import its.dao.ServiceDao;
import its.db.Database;
import its.model.OrderForm;
import its.model.OrderStatus;
import its.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Демо-запуск без установленного PostgreSQL: поднимает встроенную базу, наполняет её примерами
 * и открывает приложение. Данные живут, пока открыто окно.
 *
 * <pre>mvn test-compile exec:java "-Dexec.classpathScope=test" "-Dmain.class=its.DemoApp"</pre>
 *
 * С аргументом {@code --shots <папка>} сохраняет снимки всех вкладок в PNG и завершается.
 */
public final class DemoApp {
    private DemoApp() {
    }

    public static void main(String[] args) throws Exception {
        EmbeddedPostgres postgres = EmbeddedPostgres.start();
        Database db = new Database(postgres.getPostgresDatabase());
        db.initSchema();
        seed(db);

        MainFrame.installTheme();
        MainFrame[] frame = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new MainFrame(db);
            frame[0].setVisible(true);
        });

        if (args.length == 2 && args[0].equals("--shots")) {
            File dir = new File(args[1]);
            dir.mkdirs();
            String[] names = {"home", "sales", "expenses", "analytics", "clients", "services"};
            for (int i = 0; i < names.length; i++) {
                int tab = i;
                SwingUtilities.invokeAndWait(() -> {
                    frame[0].selectTab(tab);
                    if (tab == 0) {
                        expandFirstCard(frame[0]);
                    }
                });
                Thread.sleep(600);
                File file = new File(dir, names[i] + ".png");
                SwingUtilities.invokeAndWait(() -> snapshot(frame[0], file));
            }
            postgres.close();
            System.exit(0);
        }
    }

    private static void seed(Database db) {
        ServiceDao services = new ServiceDao(db);
        services.create("Диагностика", new BigDecimal("500"));
        services.create("Замена экрана iPhone 11", new BigDecimal("6500"));
        services.create("Замена аккумулятора", new BigDecimal("2500"));
        services.create("Чистка ноутбука от пыли", new BigDecimal("1800"));
        services.create("Защитное стекло", new BigDecimal("400"));

        OrderDao orders = new OrderDao(db);
        LocalDate today = LocalDate.now();
        orders.save(new OrderForm(null, today.minusDays(6), null, "Apple", "iPhone 11",
                "Иванов Иван Иванович", "79991234567",
                "Не включается, не реагирует на зарядку. Со слов клиента, перед этим быстро разряжался.",
                "После падения в воду", "Клиент просил позвонить до начала работ. Предоплата 1000 ₽.",
                new BigDecimal("4500")));
        orders.save(new OrderForm(null, today.minusDays(3), null, "Lenovo", "IdeaPad 5",
                "Смирнова Ольга Петровна", "79035556677", "Сильно греется и шумит", "", "", new BigDecimal("1800")));
        orders.save(new OrderForm(null, today.minusDays(1), null, "Samsung", "Galaxy A52",
                "Кузнецов Дмитрий", "79261112233", "Разбит экран", "После падения", "Оригинальный дисплей", null));
        int done1 = orders.save(new OrderForm(null, today.minusDays(12), null, "Xiaomi", "Redmi Note 10",
                "Иванов Иван Иванович", "79991234567", "Не держит заряд", "", "Заменён аккумулятор", new BigDecimal("2500")));
        int done2 = orders.save(new OrderForm(null, today.minusDays(9), null, "Asus", "VivoBook 15",
                "Попова Мария", "79167778899", "Залита клавиатура", "После пролитого чая", "", new BigDecimal("3900")));
        orders.setStatus(done1, OrderStatus.DONE);
        orders.setStatus(done2, OrderStatus.DONE);

        SaleDao sales = new SaleDao(db);
        sales.save(null, today, "Защитное стекло", 2, new BigDecimal("400"), "", null);
        sales.save(null, today, "Замена аккумулятора", 1, new BigDecimal("2500"),
                "Иванов Иван Иванович", "79991234567");
        sales.save(null, today.minusDays(4), "Чистка ноутбука от пыли", 1, new BigDecimal("1800"),
                "Попова Мария", "79167778899");
        int[] monthly = {9, 14, 11, 18, 22, 16, 25, 21, 28, 24, 31};
        for (int i = 0; i < monthly.length; i++) {
            sales.save(null, today.minusMonths(monthly.length - i).withDayOfMonth(10), "Замена экрана iPhone 11",
                    monthly[i] / 4, new BigDecimal("6500"), "", null);
            sales.save(null, today.minusMonths(monthly.length - i).withDayOfMonth(20), "Диагностика",
                    monthly[i], new BigDecimal("500"), "", null);
        }

        ExpenseDao expenses = new ExpenseDao(db);
        expenses.save(null, today, "Закупка запчастей", new BigDecimal("7400"));
        expenses.save(null, today.minusDays(2), "Интернет и связь", new BigDecimal("1200"));
        for (int i = 0; i <= monthly.length; i++) {
            expenses.save(null, today.minusMonths(i).withDayOfMonth(1), "Аренда помещения", new BigDecimal("15000"));
        }
    }

    private static boolean expandFirstCard(Container root) {
        for (Component child : root.getComponents()) {
            if (child.getClass().getSimpleName().equals("OrderCard")) {
                Component header = ((Container) child).getComponent(0);
                header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 5, 5, 1, false));
                return true;
            }
            if (child instanceof Container container) {
                if (expandFirstCard(container)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void snapshot(MainFrame frame, File file) {
        Container content = frame.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        content.paint(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", file);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
