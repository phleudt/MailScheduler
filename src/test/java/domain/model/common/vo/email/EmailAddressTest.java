package domain.model.common.vo.email;

import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailAddressTest {

    @Test
    public void shouldCreateValidEmailAddress() {
        String rawValue = "test@mail.com";
        EmailAddress emailAddress = EmailAddress.of(rawValue);
        assertNotNull(emailAddress);

        assertEquals(rawValue, emailAddress.value());
        assertEquals(rawValue, emailAddress.toString());
    }

    @Test
    public void shouldCreateInvalidEmailAddressWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of(null));
    }

    @Test
    public void shouldCreateInvalidEmailAddressWhenEmpty() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of(""));
    }

    @Test
    public void shouldCreateInvalidEmailAddressWhenBlank() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("    "));
    }

    @Test
    public void shouldCreateInvalidEmailAddressWhenInvalidEmailPattern() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@m"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("tes't@mail.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("@mail.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@mail"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@mail.c"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@@mail.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test mail@mail.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@mail.com."));
    }

    @Test
    public void shouldCreateValidEmailAddressWhenValidEmailPattern() {
        assertEquals(EmailAddress.of("test@mail.com"), EmailAddress.of("test@mail.com"));
        assertEquals(EmailAddress.of("user.name+tag+sorting@example.com"), EmailAddress.of("user.name+tag+sorting@example.com"));
        assertEquals(EmailAddress.of("user_name@example.co.uk"), EmailAddress.of("user_name@example.co.uk"));
        assertEquals(EmailAddress.of("user-name@sub.domain.com"), EmailAddress.of("user-name@sub.domain.com"));
        assertEquals(EmailAddress.of("user123@domain123.org"), EmailAddress.of("user123@domain123.org"));
        assertEquals(EmailAddress.of("a.b.c.d@mail-domain.com"), EmailAddress.of("a.b.c.d@mail-domain.com"));
        assertEquals(EmailAddress.of("user%test@domain.com"), EmailAddress.of("user%test@domain.com"));
    }

    @Test
    public void shouldEqualIfSameEmailAddress() {
        assertEquals(EmailAddress.of("test@mail.com"), EmailAddress.of("test@mail.com"));
    }

    @Test
    public void shouldEqualIfSameObject() {
        EmailAddress email = EmailAddress.of("test@mail.com");
        assertEquals(email, email);
    }

    @Test
    public void shouldNotEqualIfDifferentEmailAddressValues() {
        assertNotEquals(EmailAddress.of("test1@mail.com"), EmailAddress.of("test2@mail.com"));
    }

    @Test
    public void shouldNotEqualIfDifferentObject() {
        assertNotEquals("test@mail.com", EmailAddress.of("test@mail.com"));
    }
}
