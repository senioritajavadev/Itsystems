package its.ui;

import its.dao.ClientDao;
import its.model.Client;
import its.util.Phone;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.Optional;

/**
 * Пара полей «Мобильный» + «ФИО клиента». Как только введённый номер совпадает с номером
 * клиента из базы, ФИО подтягивается автоматически.
 */
final class ClientFields {
    final JTextField phone = new JTextField();
    final JTextField name = new JTextField();
    final JLabel hint = new JLabel(" ");

    private final ClientDao clients;
    private final boolean required;

    ClientFields(ClientDao clients, boolean required) {
        this.clients = clients;
        this.required = required;
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
        Ui.onChange(phone, this::lookup);
    }

    void set(String phoneDigits, String fullName) {
        phone.setText(Phone.format(phoneDigits));
        name.setText(fullName);
    }

    private void lookup() {
        Optional<String> normalized = Phone.normalize(phone.getText());
        if (normalized.isEmpty()) {
            hint.setText(" ");
            return;
        }
        try {
            Optional<Client> found = clients.findByPhone(normalized.get());
            if (found.isPresent()) {
                name.setText(found.get().fullName());
                hint.setForeground(Ui.SUCCESS);
                hint.setText("Клиент найден в базе — ФИО подставлено");
            } else {
                hint.setForeground(Ui.MUTED);
                hint.setText("Нового клиента добавим в базу при сохранении");
            }
        } catch (RuntimeException e) {
            // Подсказка не должна мешать вводу: об ошибке базы пользователь узнает при сохранении
            hint.setText(" ");
        }
    }

    /** Нормализованный номер; null, если клиент необязателен и номер не введён. */
    String phoneValue() {
        if (!required && phone.getText().isBlank()) {
            return null;
        }
        return Phone.normalize(phone.getText()).orElseThrow(() ->
                new IllegalArgumentException("Мобильный: введите номер полностью, например +7 999 123-45-67"));
    }

    String nameValue() {
        String value = name.getText().strip();
        if (value.isEmpty() && (required || !phone.getText().isBlank())) {
            throw new IllegalArgumentException("ФИО клиента: заполните поле");
        }
        return value;
    }
}
