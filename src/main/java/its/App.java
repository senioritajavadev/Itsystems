package its;

import its.db.Database;
import its.db.DbConfig;
import its.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class
App {
    private App() {
    }

    public static void main(String[] args) {
        MainFrame.installTheme();
        DbConfig config = DbConfig.load();
        Database db;
        try {
            db = new Database(config);
            db.initSchema();
        } catch (RuntimeException e) {
            String reason = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage() : String.valueOf(e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Не удалось подключиться к PostgreSQL.\n\n"
                            + "Адрес: " + config.url() + "\n"
                            + "Пользователь: " + config.user() + "\n\n"
                            + "Проверьте, что сервер запущен. Параметры подключения задаются в файле\n"
                            + "db.properties (url, user, password) рядом с приложением или переменными\n"
                            + "окружения ITS_DB_URL, ITS_DB_USER, ITS_DB_PASSWORD.\n\n"
                            + "Причина: " + reason,
                    "IT-S", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(db::close));
        SwingUtilities.invokeLater(() -> new MainFrame(db).setVisible(true));
    }
}
