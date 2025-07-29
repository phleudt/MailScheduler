package domain.model.email;

import com.mailscheduler.domain.model.email.EmailType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailTypeTest {

    @Test
    public void shouldCreateInitialEmailType() {
        EmailType emailType = EmailType.INITIAL;
        assertEquals(EmailType.INITIAL, emailType);
    }

    @Test
    public void shouldCreateFollowUpEmailType() {
        EmailType emailType = EmailType.FOLLOW_UP;
        assertEquals(EmailType.FOLLOW_UP, emailType);
    }

    @Test
    public void shouldCreateExternalInitialEmailType() {
        EmailType emailType = EmailType.EXTERNALLY_INITIAL;
        assertEquals(EmailType.EXTERNALLY_INITIAL, emailType);
    }

    @Test
    public void shouldCreateExternalFollowUpEmailType() {
        EmailType emailType = EmailType.EXTERNALLY_FOLLOW_UP;
        assertEquals(EmailType.EXTERNALLY_FOLLOW_UP, emailType);
    }

    @Test
    public void isFollowUpShouldReturnTrueForFollowUpEmailType() {
        EmailType emailType1 = EmailType.FOLLOW_UP;
        assertTrue(emailType1.isFollowUp());

        EmailType emailType2 = EmailType.EXTERNALLY_FOLLOW_UP;
        assertTrue(emailType2.isFollowUp());
    }

    @Test
    public void isFollowUpShouldReturnFalseForOtherEmailType() {
        EmailType emailType1 = EmailType.INITIAL;
        assertFalse(emailType1.isFollowUp());

        EmailType emailType2 = EmailType.EXTERNALLY_INITIAL;
        assertFalse(emailType2.isFollowUp());
    }

    @Test
    public void isInitialShouldReturnTrueForInitialEmailType() {
        EmailType emailType1 = EmailType.INITIAL;
        assertTrue(emailType1.isInitial());

        EmailType emailType2 = EmailType.EXTERNALLY_INITIAL;
        assertTrue(emailType2.isInitial());
    }

    @Test
    public void isInitialShouldReturnFalseForOtherEmailType() {
        EmailType emailType1 = EmailType.FOLLOW_UP;
        assertFalse(emailType1.isInitial());

        EmailType emailType2 = EmailType.EXTERNALLY_FOLLOW_UP;
        assertFalse(emailType2.isInitial());
    }

    @Test
    public void isExternalShouldReturnTrueForExternalEmailType() {
        EmailType emailType1 = EmailType.EXTERNALLY_INITIAL;
        assertTrue(emailType1.isExternal());

        EmailType emailType2 = EmailType.EXTERNALLY_FOLLOW_UP;
        assertTrue(emailType2.isExternal());
    }

    @Test
    public void isExternalShouldReturnFalseForOtherEmailType() {
        EmailType emailType1 = EmailType.INITIAL;
        assertFalse(emailType1.isExternal());

        EmailType emailType2 = EmailType.FOLLOW_UP;
        assertFalse(emailType2.isExternal());
    }

    @Test
    public void toStringShouldReturnEmailTypeString() {
        EmailType emailType1 = EmailType.INITIAL;
        assertEquals("INITIAL", emailType1.toString());

        EmailType emailType2 = EmailType.FOLLOW_UP;
        assertEquals("FOLLOW_UP", emailType2.toString());

        EmailType emailType3 = EmailType.EXTERNALLY_INITIAL;
        assertEquals("EXTERNALLY_INITIAL", emailType3.toString());

        EmailType emailType4 = EmailType.EXTERNALLY_FOLLOW_UP;
        assertEquals("EXTERNALLY_FOLLOW_UP", emailType4.toString());
    }
}
