package its.dao;

import its.db.DataException;

import java.sql.SQLException;

final class Daos {
    private Daos() {
    }

    /** Шаблон для ILIKE: пустой поиск находит всё, спецсимволы LIKE экранируются. */
    static String like(String search) {
        String s = search == null ? "" : search.strip();
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /** Нарушение ограничений БД (уникальность, внешний ключ, CHECK) превращает в понятное сообщение. */
    static DataException friendly(DataException e, String constraintMessage) {
        if (e.getCause() instanceof SQLException sql && sql.getSQLState() != null
                && sql.getSQLState().startsWith("23")) {
            return new DataException(constraintMessage, null);
        }
        return e;
    }
}
