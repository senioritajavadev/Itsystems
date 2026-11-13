package its;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import its.dao.AnalyticsDao;
import its.dao.ClientDao;
import its.dao.ExpenseDao;
import its.dao.OrderDao;
import its.dao.SaleDao;
import its.dao.ServiceDao;
import its.db.DataException;
import its.db.Database;
import its.model.Analytics;
import its.model.ClientRow;
import its.model.Expense;
import its.model.OrderForm;
import its.model.OrderStatus;
import its.model.RepairOrder;
import its.model.Sale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет SQL всех DAO на настоящем PostgreSQL (встроенном, установка не нужна). */
class DaoIntegrationTest {
    private static EmbeddedPostgres postgres;
    private static Database db;

    private final ClientDao clients = new ClientDao(db);
    private final OrderDao orders = new OrderDao(db);
    private final ServiceDao services = new ServiceDao(db);
    private final SaleDao sales = new SaleDao(db);
    private final ExpenseDao expenses = new ExpenseDao(db);

    @BeforeAll
    static void startDatabase() throws Exception {
        postgres = EmbeddedPostgres.start();
        db = new Database(postgres.getPostgresDatabase());
        db.initSchema();
        db.initSchema(); // повторный запуск приложения не должен падать на уже созданной схеме
    }

    @AfterAll
    static void stopDatabase() throws Exception {
        postgres.close();
    }

    @BeforeEach
    void clean() {
        db.update("TRUNCATE its.sales, its.orders, its.services, its.clients, its.expenses RESTART IDENTITY");
    }

    private static OrderForm form(Integer id, String name, String phone) {
        return new OrderForm(id, LocalDate.of(2026, 11, 1), null, "Apple", "iPhone 11", name, phone,
                "Не включается", "После падения", "Срочно", null);
    }

    private static OrderForm pricedForm(String price, LocalDate issued) {
        return new OrderForm(null, LocalDate.of(2020, 1, 1), issued, "Apple", "iPhone 11", "Иванов Иван",
                "79991234567", "", "", "", new BigDecimal(price));
    }

    @Test
    void newOrderCreatesClientAndAppearsInProgress() {
        int id = orders.save(form(null, "Иванов Иван Иванович", "79991234567"));

        List<RepairOrder> inProgress = orders.findByStatus(OrderStatus.IN_PROGRESS, "");
        assertEquals(1, inProgress.size());
        RepairOrder order = inProgress.get(0);
        assertEquals(id, order.id());
        assertEquals("Apple iPhone 11", order.device());
        assertEquals("Иванов Иван Иванович", order.clientName());
        assertEquals("79991234567", order.clientPhone());
        assertEquals("Не включается", order.defect());
        assertEquals("После падения", order.afterWhat());
        assertEquals("Срочно", order.notes());
        assertNull(order.issuedDate());
        assertTrue(orders.findByStatus(OrderStatus.DONE, "").isEmpty());

        assertEquals("Иванов Иван Иванович", clients.findByPhone("79991234567").orElseThrow().fullName());
    }

    @Test
    void samePhoneReusesClientInsteadOfDuplicating() {
        orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.save(form(null, "Иванов Иван", "79991234567"));

        List<ClientRow> all = clients.findAll("");
        assertEquals(1, all.size());
        assertEquals(2, all.get(0).ordersCount());
    }

    @Test
    void unknownPhoneIsNotFound() {
        assertTrue(clients.findByPhone("70000000000").isEmpty());
    }

    @Test
    void statusChangeMovesOrderAndManagesIssueDate() {
        int id = orders.save(form(null, "Иванов Иван", "79991234567"));

        orders.setStatus(id, OrderStatus.DONE);
        RepairOrder done = orders.findByStatus(OrderStatus.DONE, "").get(0);
        assertEquals(LocalDate.now(), done.issuedDate());
        assertTrue(orders.findByStatus(OrderStatus.IN_PROGRESS, "").isEmpty());

        orders.setStatus(id, OrderStatus.IN_PROGRESS);
        assertNull(orders.findByStatus(OrderStatus.IN_PROGRESS, "").get(0).issuedDate());
    }

    @Test
    void orderEditKeepsNumberAndUpdatesFields() {
        int id = orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.save(new OrderForm(id, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5), "Samsung", "A52",
                "Петров Пётр", "79990000000", "Разбит экран", "", "", new BigDecimal("3200.50")));

        RepairOrder order = orders.findByStatus(OrderStatus.IN_PROGRESS, "").get(0);
        assertEquals(id, order.id());
        assertEquals("Samsung A52", order.device());
        assertEquals("Петров Пётр", order.clientName());
        assertEquals(LocalDate.of(2026, 11, 5), order.issuedDate());
        assertEquals(new BigDecimal("3200.50"), order.price());

        orders.save(form(id, "Петров Пётр", "79990000000"));
        assertNull(orders.findByStatus(OrderStatus.IN_PROGRESS, "").get(0).price());
    }

    @Test
    void searchMatchesDeviceClientPhoneAndIgnoresLikeWildcards() {
        orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.save(new OrderForm(null, LocalDate.now(), null, "Xiaomi", "Redmi 9", "Сидорова Анна",
                "79995556677", "", "", "", null));

        assertEquals(1, orders.findByStatus(OrderStatus.IN_PROGRESS, "redmi").size());
        assertEquals(1, orders.findByStatus(OrderStatus.IN_PROGRESS, "сидор").size());
        assertEquals(1, orders.findByStatus(OrderStatus.IN_PROGRESS, "5556677").size());
        assertEquals(0, orders.findByStatus(OrderStatus.IN_PROGRESS, "%").size());
        assertEquals(2, orders.findByStatus(OrderStatus.IN_PROGRESS, "  ").size());
    }

    @Test
    void clientWithOrdersCannotBeDeletedButFreeClientCan() {
        orders.save(form(null, "Иванов Иван", "79991234567"));
        clients.create("Свободный Клиент", "79990001122");
        List<ClientRow> all = clients.findAll("");
        int withOrders = all.stream().filter(r -> r.ordersCount() == 1).findFirst().orElseThrow().client().id();
        int free = all.stream().filter(r -> r.ordersCount() == 0).findFirst().orElseThrow().client().id();

        DataException e = assertThrows(DataException.class, () -> clients.delete(withOrders));
        assertEquals("Нельзя удалить клиента, у которого есть заказы", e.getMessage());
        clients.delete(free);
        assertEquals(1, clients.findAll("").size());
    }

    @Test
    void duplicatePhoneAndServiceNameGiveReadableErrors() {
        clients.create("Первый", "79990001122");
        assertEquals("Клиент с таким номером уже есть в базе",
                assertThrows(DataException.class, () -> clients.create("Второй", "79990001122")).getMessage());

        services.create("Диагностика", new BigDecimal("500.00"));
        assertEquals("Услуга с таким названием уже есть",
                assertThrows(DataException.class, () -> services.create("Диагностика", BigDecimal.TEN))
                        .getMessage());
    }

    @Test
    void servicesCrud() {
        services.create("Замена экрана", new BigDecimal("4500.00"));
        int id = services.findAll("экран").get(0).id();
        services.update(id, "Замена дисплея", new BigDecimal("5000.50"));
        assertEquals(new BigDecimal("5000.50"), services.findAll("").get(0).price());
        services.delete(id);
        assertTrue(services.findAll("").isEmpty());
    }

    @Test
    void salesWithAndWithoutClient() {
        sales.save(null, LocalDate.now(), "Защитное стекло", 2, new BigDecimal("350.00"), "", null);
        sales.save(null, LocalDate.now(), "Замена экрана", 1, new BigDecimal("4500.00"),
                "Иванов Иван", "79991234567");

        List<Sale> all = sales.findAll("");
        assertEquals(2, all.size());
        Sale glass = sales.findAll("стекло").get(0);
        assertEquals(new BigDecimal("700.00"), glass.total());
        assertEquals("", glass.clientName());
        assertEquals("Иванов Иван", sales.findAll("иванов").get(0).clientName());
        assertNotNull(clients.findByPhone("79991234567").orElse(null));

        sales.save(glass.id(), glass.date(), "Стекло 3D", 3, new BigDecimal("400.00"), "", null);
        assertEquals(new BigDecimal("1200.00"), sales.findAll("3D").get(0).total());
        sales.delete(glass.id());
        assertEquals(1, sales.findAll("").size());
    }

    @Test
    void analyticsAggregatesSalesAndOrders() {
        LocalDate today = LocalDate.now();
        sales.save(null, today, "Замена экрана", 1, new BigDecimal("4500.00"), "", null);
        sales.save(null, today, "Чехол", 2, new BigDecimal("500.00"), "", null);
        sales.save(null, today.minusYears(2), "Чехол", 1, new BigDecimal("300.00"), "", null);
        int id = orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.setStatus(id, OrderStatus.DONE);

        Analytics a = new AnalyticsDao(db).load();
        assertEquals(new BigDecimal("5500.00"), a.revenueToday());
        assertEquals(new BigDecimal("5500.00"), a.revenueMonth());
        assertEquals(new BigDecimal("5500.00"), a.revenueYear());
        assertEquals(new BigDecimal("5800.00"), a.revenueTotal());
        assertEquals(3, a.salesCount());
        assertEquals(new BigDecimal("1933.33"), a.averageCheck());
        assertEquals(1, a.ordersInProgress());
        assertEquals(1, a.ordersDone());

        assertEquals(12, a.months().size());
        Analytics.MonthRevenue last = a.months().get(11);
        assertEquals(today.withDayOfMonth(1), last.month());
        assertEquals(new BigDecimal("5500.00"), last.revenue());
        assertEquals(0, a.months().get(0).revenue().signum());

        assertEquals("Замена экрана", a.topItems().get(0).title());
        assertEquals("Чехол", a.topItems().get(1).title());
        assertEquals(3, a.topItems().get(1).quantity());
        assertEquals(new BigDecimal("1300.00"), a.topItems().get(1).revenue());
    }

    @Test
    void analyticsCountsDoneOrdersWithPriceByIssueDate() {
        LocalDate today = LocalDate.now();
        sales.save(null, today, "Чехол", 1, new BigDecimal("500.00"), "", null);
        int doneToday = orders.save(pricedForm("3000.00", null));
        orders.setStatus(doneToday, OrderStatus.DONE);
        int doneLongAgo = orders.save(pricedForm("1000.00", today.minusYears(2)));
        orders.setStatus(doneLongAgo, OrderStatus.DONE);
        orders.save(pricedForm("7000.00", null));
        int doneWithoutPrice = orders.save(form(null, "Иванов Иван", "79991234567"));
        orders.setStatus(doneWithoutPrice, OrderStatus.DONE);

        Analytics a = new AnalyticsDao(db).load();
        assertEquals(new BigDecimal("3500.00"), a.revenueToday());
        assertEquals(new BigDecimal("3500.00"), a.revenueYear());
        assertEquals(new BigDecimal("4500.00"), a.revenueTotal());
        assertEquals(3, a.salesCount());
        assertEquals(new BigDecimal("3500.00"), a.months().get(11).revenue());
        assertEquals("Ремонт: Apple iPhone 11", a.topItems().get(0).title());
        assertEquals(2, a.topItems().get(0).quantity());
        assertEquals(new BigDecimal("4000.00"), a.topItems().get(0).revenue());

        orders.setStatus(doneToday, OrderStatus.IN_PROGRESS);
        assertEquals(new BigDecimal("500.00"), new AnalyticsDao(db).load().revenueToday());
    }

    @Test
    void expensesCrud() {
        LocalDate today = LocalDate.now();
        expenses.save(null, today, "Аренда помещения", new BigDecimal("15000.00"));
        expenses.save(null, today.minusDays(1), "Запчасти", new BigDecimal("700.50"));

        assertEquals(2, expenses.findAll("").size());
        assertEquals("Аренда помещения", expenses.findAll("").get(0).title());
        Expense parts = expenses.findAll("запч").get(0);
        expenses.save(parts.id(), today, "Запчасти и расходники", new BigDecimal("900.00"));
        assertEquals(new BigDecimal("900.00"), expenses.findAll("расходники").get(0).amount());
        expenses.delete(parts.id());
        assertEquals(1, expenses.findAll("").size());
    }

    @Test
    void analyticsSubtractsExpensesFromRevenue() {
        LocalDate today = LocalDate.now();
        sales.save(null, today, "Замена экрана", 1, new BigDecimal("4500.00"), "", null);
        sales.save(null, today, "Чехол", 2, new BigDecimal("500.00"), "", null);
        expenses.save(null, today, "Запчасти", new BigDecimal("2000.00"));
        expenses.save(null, today, "Интернет", new BigDecimal("500.00"));
        expenses.save(null, today.minusYears(2), "Аренда", new BigDecimal("10000.00"));

        Analytics a = new AnalyticsDao(db).load();
        assertEquals(new BigDecimal("2500.00"), a.expensesMonth());
        assertEquals(new BigDecimal("2500.00"), a.expensesYear());
        assertEquals(new BigDecimal("12500.00"), a.expensesTotal());
        assertEquals(new BigDecimal("3000.00"), a.profitMonth());
        assertEquals(new BigDecimal("3000.00"), a.profitYear());
        assertEquals(new BigDecimal("-7000.00"), a.profitTotal());

        // Две продажи и два расхода одного месяца не должны перемножиться
        Analytics.MonthRevenue last = a.months().get(11);
        assertEquals(new BigDecimal("5500.00"), last.revenue());
        assertEquals(new BigDecimal("2500.00"), last.expenses());
        assertEquals(new BigDecimal("3000.00"), last.profit());
        assertEquals(0, a.months().get(0).expenses().signum());
    }

    @Test
    void analyticsOnEmptyDatabase() {
        Analytics a = new AnalyticsDao(db).load();
        assertEquals(0, a.expensesTotal().signum());
        assertEquals(0, a.profitTotal().signum());
        assertEquals(0, a.revenueTotal().signum());
        assertEquals(0, a.averageCheck().signum());
        assertEquals(12, a.months().size());
        assertTrue(a.topItems().isEmpty());
    }
}
