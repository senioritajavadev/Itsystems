package its.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class Database implements AutoCloseable {
    private final DataSource dataSource;
    private final AutoCloseable closer;

    public Database(DbConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(3);
        hikari.setConnectionTimeout(5000);
        hikari.setPoolName("its");
        HikariDataSource ds = new HikariDataSource(hikari);
        this.dataSource = ds;
        this.closer = ds;
    }

    /** Для тестов и демо: работа поверх готового DataSource. */
    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
        this.closer = () -> { };
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Создаёт схему и таблицы, если их ещё нет. */
    public void initSchema() {
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/its/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Не найден ресурс /its/schema.sql");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataException("Не удалось прочитать schema.sql", e);
        }
        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\n")) {
            if (!line.strip().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String sql : withoutComments.toString().split(";")) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Не удалось создать таблицы IT-S", e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = getConnection()) {
            return query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataException("Ошибка чтения из базы данных", e);
        }
    }

    public int update(String sql, Object... params) {
        try (Connection c = getConnection()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw new DataException("Ошибка записи в базу данных", e);
        }
    }

    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() {
        try {
            closer.close();
        } catch (Exception ignored) {
            // закрываемся при выходе из приложения — сообщать уже некому
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
