package its.ui;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import its.dao.ClientDao;
import its.db.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Требование README: при вводе мобильного, который уже есть в базе, клиент подтягивается в форму. */
class ClientFieldsTest {
    private static EmbeddedPostgres postgres;
    private static ClientDao clients;

    @BeforeAll
    static void startDatabase() throws Exception {
        postgres = EmbeddedPostgres.start();
        Database db = new Database(postgres.getPostgresDatabase());
        db.initSchema();
        clients = new ClientDao(db);
        clients.create("Иванов Иван Иванович", "79991234567");
    }

    @AfterAll
    static void stopDatabase() throws Exception {
        postgres.close();
    }

    @Test
    void knownPhoneFillsNameInAnyInputFormat() {
        ClientFields fields = new ClientFields(clients, true);
        fields.phone.setText("8 (999) 123-45-67");

        assertEquals("Иванов Иван Иванович", fields.name.getText());
        assertTrue(fields.hint.getText().contains("найден"));
        assertEquals("79991234567", fields.phoneValue());
    }

    @Test
    void nameIsNotTouchedWhilePhoneIsIncompleteOrUnknown() {
        ClientFields fields = new ClientFields(clients, true);
        fields.name.setText("Новый Клиент");

        fields.phone.setText("+7 999 123");
        assertEquals("Новый Клиент", fields.name.getText());

        fields.phone.setText("+7 900 000-00-00");
        assertEquals("Новый Клиент", fields.name.getText());
        assertTrue(fields.hint.getText().contains("добавим"));
    }

    @Test
    void validation() {
        ClientFields required = new ClientFields(clients, true);
        assertThrows(IllegalArgumentException.class, required::phoneValue);
        assertThrows(IllegalArgumentException.class, required::nameValue);

        ClientFields optional = new ClientFields(clients, false);
        assertNull(optional.phoneValue());
        assertEquals("", optional.nameValue());
        optional.phone.setText("123");
        assertThrows(IllegalArgumentException.class, optional::phoneValue);
    }
}
