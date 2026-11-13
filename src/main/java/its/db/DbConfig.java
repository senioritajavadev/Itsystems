package its.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Параметры подключения. Приоритет: переменные окружения ITS_DB_URL / ITS_DB_USER / ITS_DB_PASSWORD,
 * затем файл db.properties в рабочей папке (ключи url, user, password), затем значения по умолчанию.
 */
public record DbConfig(String url, String user, String password) {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    public static DbConfig load() {
        Properties file = new Properties();
        Path path = Path.of("db.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                file.load(in);
            } catch (IOException e) {
                throw new DataException("Не удалось прочитать db.properties", e);
            }
        }
        return new DbConfig(
                pick("ITS_DB_URL", file.getProperty("url"), DEFAULT_URL),
                pick("ITS_DB_USER", file.getProperty("user"), DEFAULT_USER),
                pick("ITS_DB_PASSWORD", file.getProperty("password"), DEFAULT_PASSWORD));
    }

    private static String pick(String envName, String fileValue, String fallback) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return fileValue != null && !fileValue.isBlank() ? fileValue.trim() : fallback;
    }
}
