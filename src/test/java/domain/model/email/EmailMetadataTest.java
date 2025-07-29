package domain.model.email;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.model.recipient.Recipient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EmailMetadataTest {

    private static final EntityId<com.mailscheduler.domain.model.email.Email> EMAIL_ID = EntityId.of(1L);
    private static final EntityId<Recipient> RECIPIENT_ID = EntityId.of(2L);
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    public void shouldCreateValidPendingMetadata() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, null, TODAY, null
        );
        assertEquals(EMAIL_ID, metadata.initialEmailId());
        assertEquals(RECIPIENT_ID, metadata.recipientId());
        assertEquals(0, metadata.followupNumber());
        assertEquals(EmailStatus.PENDING, metadata.status());
        assertNull(metadata.failureReason());
        assertEquals(TODAY, metadata.scheduledDate());
        assertNull(metadata.sentDate());
        assertTrue(metadata.isPending());
        assertFalse(metadata.hasFailed());
        assertFalse(metadata.hasBeenSent());
        assertFalse(metadata.isFollowUp());
        assertEquals(Optional.empty(), metadata.getFailureReason());
    }

    @Test
    public void shouldCreateValidSentMetadata() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.SENT, null, TODAY, TODAY
        );
        assertTrue(metadata.hasBeenSent());
        assertFalse(metadata.hasFailed());
        assertFalse(metadata.isPending());
    }

    @Test
    public void shouldCreateValidFailedMetadata() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.FAILED, "SMTP error", TODAY, null
        );
        assertTrue(metadata.hasFailed());
        assertFalse(metadata.hasBeenSent());
        assertFalse(metadata.isPending());
        assertEquals(Optional.of("SMTP error"), metadata.getFailureReason());
    }

    @Test
    public void shouldCreateValidCancelledMetadata() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.CANCELLED, null, TODAY, null
        );
        assertEquals(EmailStatus.CANCELLED, metadata.status());
    }

    @Test
    public void shouldThrowIfStatusIsNull() {
        assertThrows(NullPointerException.class, () ->
                new EmailMetadata(EMAIL_ID, RECIPIENT_ID, 0, null, null, TODAY, null)
        );
    }

    @Test
    public void shouldThrowIfFailureReasonMissingForFailed() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmailMetadata(EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.FAILED, null, TODAY, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new EmailMetadata(EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.FAILED, "   ", TODAY, null)
        );
    }

    @Test
    public void shouldThrowIfSentDateMissingForSent() {
        assertThrows(IllegalArgumentException.class, () ->
                new EmailMetadata(EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.SENT, null, TODAY, null)
        );
    }

    @Test
    public void shouldNormalizeBlankFailureReasonToNull() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, "   ", TODAY, null
        );
        assertNull(metadata.failureReason());
    }

    @Test
    public void shouldDetectFollowUp() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 2, EmailStatus.PENDING, null, TODAY, null
        );
        assertTrue(metadata.isFollowUp());
    }

    @Test
    public void markAsSentShouldUpdateStatusAndSentDate() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, null, TODAY, null
        );
        EmailMetadata sent = metadata.markAsSent();
        assertEquals(EmailStatus.SENT, sent.status());
        assertNotNull(sent.sentDate());
        assertNull(sent.failureReason());
    }

    @Test
    public void markAsFailedShouldUpdateStatusAndReason() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, null, TODAY, null
        );
        EmailMetadata failed = metadata.markAsFailed("Network error");
        assertEquals(EmailStatus.FAILED, failed.status());
        assertEquals("Network error", failed.failureReason());
        assertNull(failed.sentDate());
    }

    @Test
    public void cancelShouldSetStatusToCancelled() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, null, TODAY, null
        );
        EmailMetadata cancelled = metadata.cancel();
        assertEquals(EmailStatus.CANCELLED, cancelled.status());
    }

    @Test
    public void rescheduleShouldUpdateScheduledDateIfPending() {
        EmailMetadata metadata = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.PENDING, null, TODAY, null
        );
        LocalDate newDate = TODAY.plusDays(2);
        EmailMetadata rescheduled = metadata.reschedule(newDate);
        assertEquals(newDate, rescheduled.scheduledDate());
    }

    @Test
    public void rescheduleShouldThrowIfNotPending() {
        EmailMetadata sent = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 0, EmailStatus.SENT, null, TODAY, TODAY
        );
        assertThrows(IllegalStateException.class, () -> sent.reschedule(TODAY.plusDays(1)));
    }

    @Test
    public void builderShouldBuildValidMetadata() {
        EmailMetadata metadata = new EmailMetadata.Builder()
                .initialEmailId(EMAIL_ID)
                .recipientId(RECIPIENT_ID)
                .followupNumber(1)
                .status(EmailStatus.PENDING)
                .scheduledDate(TODAY)
                .build();
        assertEquals(EMAIL_ID, metadata.initialEmailId());
        assertEquals(RECIPIENT_ID, metadata.recipientId());
        assertEquals(1, metadata.followupNumber());
        assertEquals(EmailStatus.PENDING, metadata.status());
        assertEquals(TODAY, metadata.scheduledDate());
    }

    @Test
    public void builderFromShouldCopyValues() {
        EmailMetadata original = new EmailMetadata(
                EMAIL_ID, RECIPIENT_ID, 1, EmailStatus.PENDING, null, TODAY, null
        );
        EmailMetadata copy = new EmailMetadata.Builder().from(original).build();
        assertEquals(original, copy);
    }

    @Test
    public void builderCreateInitialShouldSetDefaults() {
        EmailMetadata metadata = EmailMetadata.Builder.createInitial(RECIPIENT_ID, TODAY);
        assertEquals(RECIPIENT_ID, metadata.recipientId());
        assertEquals(0, metadata.followupNumber());
        assertEquals(EmailStatus.PENDING, metadata.status());
        assertEquals(TODAY, metadata.scheduledDate());
    }

    @Test
    public void builderCreateFollowUpShouldSetFields() {
        EmailMetadata metadata = EmailMetadata.Builder.createFollowUp(
                EMAIL_ID, RECIPIENT_ID, 2, TODAY
        );
        assertEquals(EMAIL_ID, metadata.initialEmailId());
        assertEquals(RECIPIENT_ID, metadata.recipientId());
        assertEquals(2, metadata.followupNumber());
        assertEquals(EmailStatus.PENDING, metadata.status());
        assertEquals(TODAY, metadata.scheduledDate());
    }
}