package its.model;

public enum OrderStatus {
    IN_PROGRESS("В работе"),
    DONE("Выполнено");

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
