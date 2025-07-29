package domain.model.email;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailTest {

    private static final EntityId<Email> EMAIL_ID = EntityId.of(1L);
    private static final EmailAddress SENDER = EmailAddress.of("sender@mail.com");
    private static final EmailAddress RECIPIENT = EmailAddress.of("recipient@mail.com");
    private static final Subject SUBJECT = new Subject("Test Subject");
    private static final Body BODY = new Body("Test Body");
    private static final EmailType TYPE = EmailType.INITIAL;

    @Test
    public void shouldConstructWithAllFields() {
        Email email = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        assertEquals(EMAIL_ID, email.getId());
        assertEquals(SENDER, email.getSender());
        assertEquals(RECIPIENT, email.getRecipient());
        assertEquals(SUBJECT, email.getSubject());
        assertEquals(BODY, email.getBody());
        assertEquals(TYPE, email.getType());
    }

    @Test
    public void shouldConstructWithoutId() {
        Email email = new Email(SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        assertNull(email.getId());
        assertEquals(SENDER, email.getSender());
        assertEquals(RECIPIENT, email.getRecipient());
        assertEquals(SUBJECT, email.getSubject());
        assertEquals(BODY, email.getBody());
        assertEquals(TYPE, email.getType());
    }

    @Test
    public void isInitialEmailShouldReturnTrueForInitialType() {
        Email email = new Email(SENDER, RECIPIENT, SUBJECT, BODY, EmailType.INITIAL);
        assertTrue(email.isInitialEmail());
        assertFalse(email.isFollowUp());
    }

    @Test
    public void isFollowUpShouldReturnTrueForFollowUpType() {
        Email email = new Email(SENDER, RECIPIENT, SUBJECT, BODY, EmailType.FOLLOW_UP);
        assertTrue(email.isFollowUp());
        assertFalse(email.isInitialEmail());
    }

    @Test
    public void withRecipientShouldReturnNewEmailWithUpdatedRecipient() {
        Email email = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        EmailAddress newRecipient = EmailAddress.of("other@mail.com");
        Email updated = email.withRecipient(newRecipient);
        assertEquals(newRecipient, updated.getRecipient());
        assertEquals(email.getSender(), updated.getSender());
        assertEquals(email.getId(), updated.getId());
        assertEquals(email.getSubject(), updated.getSubject());
        assertEquals(email.getBody(), updated.getBody());
        assertEquals(email.getType(), updated.getType());
        assertNotSame(email, updated);
    }

    @Test
    public void equalsAndHashCodeShouldWorkForSameValues() {
        Email email1 = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        Email email2 = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentFields() {
        Email email1 = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        Email email2 = new Email(EMAIL_ID, SENDER, EmailAddress.of("other@mail.com"), SUBJECT, BODY, TYPE);
        assertNotEquals(email1, email2);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        Email email = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        String str = email.toString();
        assertTrue(str.contains("id=" + EMAIL_ID));
        assertTrue(str.contains("sender=" + SENDER));
        assertTrue(str.contains("recipient=" + RECIPIENT));
        assertTrue(str.contains("subject=" + SUBJECT));
        assertTrue(str.contains("type=" + TYPE));
    }

    @Test
    public void builderShouldBuildValidEmail() {
        Email email = new Email.Builder()
                .setId(EMAIL_ID)
                .setSenderEmail(SENDER)
                .setRecipientEmail(RECIPIENT)
                .setSubject(SUBJECT)
                .setBody(BODY)
                .setType(TYPE)
                .build();
        assertEquals(EMAIL_ID, email.getId());
        assertEquals(SENDER, email.getSender());
        assertEquals(RECIPIENT, email.getRecipient());
        assertEquals(SUBJECT, email.getSubject());
        assertEquals(BODY, email.getBody());
        assertEquals(TYPE, email.getType());
    }

    @Test
    public void builderFromShouldCopyValues() {
        Email original = new Email(EMAIL_ID, SENDER, RECIPIENT, SUBJECT, BODY, TYPE);
        Email copy = new Email.Builder().from(original).build();
        assertEquals(original, copy);
    }

    @Test
    public void builderShouldAcceptStringSubjectAndBody() {
        Email email = new Email.Builder()
                .setSenderEmail(SENDER)
                .setRecipientEmail(RECIPIENT)
                .setSubject("String Subject")
                .setBody("String Body")
                .setType(TYPE)
                .build();
        assertEquals(new Subject("String Subject"), email.getSubject());
        assertEquals(new Body("String Body"), email.getBody());
    }
}