package domain.model.recipient;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.common.vo.ThreadId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecipientMetadataTest {

    private static final EntityId<Contact> CONTACT_ID = EntityId.of(1L);
    private static final EntityId<FollowUpPlan> PLAN_ID = EntityId.of(2L);
    private static final ThreadId THREAD_ID = new ThreadId("thread-abc");

    private RecipientMetadata.Builder defaultBuilder() {
        return new RecipientMetadata.Builder()
                .contactId(CONTACT_ID)
                .followupPlanId(PLAN_ID)
                .threadId(THREAD_ID);
    }

    @Test
    public void shouldCreateValidMetadata() {
        RecipientMetadata metadata = new RecipientMetadata(CONTACT_ID, PLAN_ID, THREAD_ID);
        assertEquals(CONTACT_ID, metadata.contactId());
        assertEquals(PLAN_ID, metadata.followupPlanId());
        assertEquals(THREAD_ID, metadata.threadId());
        assertTrue(metadata.getFollowupPlanId().isPresent());
        assertTrue(metadata.getThreadId().isPresent());
    }

    @Test
    public void shouldAllowNullFollowupPlanIdAndThreadId() {
        RecipientMetadata metadata = new RecipientMetadata(CONTACT_ID, null, null);
        assertEquals(CONTACT_ID, metadata.contactId());
        assertNull(metadata.followupPlanId());
        assertNull(metadata.threadId());
        assertTrue(metadata.getFollowupPlanId().isEmpty());
        assertTrue(metadata.getThreadId().isEmpty());
    }

    @Test
    public void shouldThrowIfContactIdIsNull() {
        assertThrows(NullPointerException.class, () -> new RecipientMetadata(null, PLAN_ID, THREAD_ID));
    }

    @Test
    public void withThreadIdShouldReturnNewInstanceWithUpdatedThreadId() {
        RecipientMetadata metadata = new RecipientMetadata(CONTACT_ID, PLAN_ID, null);
        RecipientMetadata updated = metadata.withThreadId(THREAD_ID);
        assertEquals(THREAD_ID, updated.threadId());
        assertEquals(CONTACT_ID, updated.contactId());
        assertEquals(PLAN_ID, updated.followupPlanId());
        assertNull(metadata.threadId());
        assertNotSame(metadata, updated);
    }

    @Test
    public void builderShouldBuildValidMetadata() {
        RecipientMetadata metadata = defaultBuilder().build();
        assertEquals(CONTACT_ID, metadata.contactId());
        assertEquals(PLAN_ID, metadata.followupPlanId());
        assertEquals(THREAD_ID, metadata.threadId());
    }

    @Test
    public void builderShouldAllowPartialFields() {
        RecipientMetadata metadata = new RecipientMetadata.Builder()
                .contactId(CONTACT_ID)
                .build();
        assertEquals(CONTACT_ID, metadata.contactId());
        assertNull(metadata.followupPlanId());
        assertNull(metadata.threadId());
    }

    @Test
    public void builderFromShouldCopyValues() {
        RecipientMetadata original = new RecipientMetadata(CONTACT_ID, PLAN_ID, THREAD_ID);
        RecipientMetadata copy = RecipientMetadata.Builder.from(original).build();
        assertEquals(original, copy);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        RecipientMetadata metadata = defaultBuilder().build();
        String str = metadata.toString();
        assertTrue(str.contains("contactId=" + CONTACT_ID));
        assertTrue(str.contains("followupPlanId=" + PLAN_ID));
        assertTrue(str.contains("threadId=" + THREAD_ID));
    }

    @Test
    public void equalsAndHashCodeShouldWorkForSameValues() {
        RecipientMetadata m1 = new RecipientMetadata(CONTACT_ID, PLAN_ID, THREAD_ID);
        RecipientMetadata m2 = new RecipientMetadata(CONTACT_ID, PLAN_ID, THREAD_ID);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentFields() {
        RecipientMetadata m1 = new RecipientMetadata(CONTACT_ID, PLAN_ID, THREAD_ID);
        RecipientMetadata m2 = new RecipientMetadata(CONTACT_ID, null, THREAD_ID);
        assertNotEquals(m1, m2);
    }

    @Test
    public void equalsShouldReturnTrueForSameInstance() {
        RecipientMetadata metadata = defaultBuilder().build();
        assertEquals(metadata, metadata);
    }

    @Test
    public void equalsShouldReturnFalseForOtherType() {
        RecipientMetadata metadata = defaultBuilder().build();
        assertNotEquals(metadata, new Object());
    }
}