package domain.model.email;

import com.mailscheduler.domain.model.email.EmailStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailStatusTest {

    @Test
    public void shouldCreatePendingStatus() {
        EmailStatus status = EmailStatus.PENDING;
        assertEquals(EmailStatus.PENDING, status);
    }

    @Test
    public void shouldCreateSentStatus() {
        EmailStatus status = EmailStatus.SENT;
        assertEquals(EmailStatus.SENT, status);
    }

    @Test
    public void shouldCreateFailedStatus() {
        EmailStatus status = EmailStatus.FAILED;
        assertEquals(EmailStatus.FAILED, status);
    }

    @Test
    public void shouldCreateCancelledStatus() {
        EmailStatus status = EmailStatus.CANCELLED;
        assertEquals(EmailStatus.CANCELLED, status);
    }

    @Test
    public void toStringShouldReturnStatusString() {
        assertEquals("PENDING", EmailStatus.PENDING.toString());
        assertEquals("SENT", EmailStatus.SENT.toString());
        assertEquals("FAILED", EmailStatus.FAILED.toString());
        assertEquals("CANCELLED", EmailStatus.CANCELLED.toString());
    }
}