package domain.model.recipient;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.recipient.Recipient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RecipientTest {

    private static final EntityId<Recipient> RECIPIENT_ID = EntityId.of(1L);
    private static final EmailAddress EMAIL = EmailAddress.of("test@mail.com");
    private static final String SALUTATION = "Test Salutation";
    private static final boolean HAS_REPLIED = true;
    private static final LocalDate CONTACT_DATE = LocalDate.of(2024, 6, 1);

    private Recipient.Builder defaultBuilder() {
        return new Recipient.Builder()
                .setId(RECIPIENT_ID)
                .setEmailAddress(EMAIL)
                .setSalutation(SALUTATION)
                .setHasReplied(HAS_REPLIED)
                .setInitialContactDate(CONTACT_DATE);
    }

    @Test
    public void shouldBuildValidRecipient() {
        Recipient recipient = defaultBuilder().build();
        assertNotNull(recipient);
        assertEquals(RECIPIENT_ID, recipient.getId());
        assertEquals(EMAIL, recipient.getEmailAddress());
        assertEquals(SALUTATION, recipient.getSalutation());
        assertTrue(recipient.hasReplied());
        assertEquals(CONTACT_DATE, recipient.getInitialContactDate());
    }

    @Test
    public void shouldAllowNullSalutationAndContactDate() {
        Recipient recipient = new Recipient.Builder()
                .setId(RECIPIENT_ID)
                .setEmailAddress(EMAIL)
                .setHasReplied(false)
                .build();
        assertNull(recipient.getSalutation());
        assertNull(recipient.getInitialContactDate());
        assertFalse(recipient.hasBeenContacted());
    }

    @Test
    public void hasBeenContactedShouldReturnTrueIfContactDateSet() {
        Recipient recipient = defaultBuilder().build();
        assertTrue(recipient.hasBeenContacted());
    }

    @Test
    public void hasBeenContactedShouldReturnFalseIfContactDateNull() {
        Recipient recipient = new Recipient.Builder()
                .setId(RECIPIENT_ID)
                .setEmailAddress(EMAIL)
                .setHasReplied(false)
                .build();
        assertFalse(recipient.hasBeenContacted());
    }

    @Test
    public void setInitialContactDateShouldThrowIfAlreadySet() {
        Recipient recipient = defaultBuilder().build();
        assertThrows(IllegalStateException.class, () -> recipient.setInitialContactDate(LocalDate.now()));
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        Recipient r1 = defaultBuilder().build();
        Recipient r2 = defaultBuilder().build();
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentId() {
        Recipient r1 = defaultBuilder().build();
        Recipient r2 = new Recipient.Builder()
                .setId(EntityId.of(2L))
                .setEmailAddress(EMAIL)
                .setSalutation(SALUTATION)
                .setHasReplied(HAS_REPLIED)
                .setInitialContactDate(CONTACT_DATE)
                .build();
        assertNotEquals(r1, r2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentFields() {
        Recipient r1 = defaultBuilder().build();
        Recipient r2 = new Recipient.Builder()
                .setId(RECIPIENT_ID)
                .setEmailAddress(EmailAddress.of("other@mail.com"))
                .setSalutation(SALUTATION)
                .setHasReplied(HAS_REPLIED)
                .setInitialContactDate(CONTACT_DATE)
                .build();
        assertNotEquals(r1, r2);
    }

    @Test
    public void equalsShouldReturnTrueForSameInstance() {
        Recipient recipient = defaultBuilder().build();
        assertEquals(recipient, recipient);
    }

    @Test
    public void equalsShouldReturnFalseForOtherType() {
        Recipient recipient = defaultBuilder().build();
        assertNotEquals(recipient, new Object());
    }

    @Test
    public void toStringShouldContainKeyFields() {
        Recipient recipient = defaultBuilder().build();
        String str = recipient.toString();
        assertTrue(str.contains("id=" + RECIPIENT_ID));
        assertTrue(str.contains("emailAddress=" + EMAIL));
        assertTrue(str.contains("salutation='" + SALUTATION + "'"));
        assertTrue(str.contains("hasReplied=true"));
        assertTrue(str.contains("initialContactDate=" + CONTACT_DATE));
    }

    @Test
    public void builderFromShouldCopyValues() {
        Recipient original = defaultBuilder().build();
        Recipient copy = new Recipient.Builder().from(original).build();
        assertEquals(original, copy);
    }
}