package domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpStepMetadata;
import com.mailscheduler.domain.model.template.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FollowUpStepMetadataTest {

    private static final EntityId<FollowUpPlan> PLAN_ID = EntityId.of(1L);
    private static final EntityId<Template> TEMPLATE_ID = EntityId.of(2L);

    @Test
    public void shouldCreateWithPlanAndTemplateId() {
        FollowUpStepMetadata meta = new FollowUpStepMetadata(PLAN_ID, TEMPLATE_ID);
        assertEquals(PLAN_ID, meta.planId());
        assertEquals(TEMPLATE_ID, meta.templateId());
    }

    @Test
    public void shouldCreateWithNullTemplateId() {
        FollowUpStepMetadata meta = new FollowUpStepMetadata(PLAN_ID, null);
        assertEquals(PLAN_ID, meta.planId());
        assertNull(meta.templateId());
    }

    @Test
    public void shouldThrowIfPlanIdNull() {
        assertThrows(NullPointerException.class, () -> new FollowUpStepMetadata(null, TEMPLATE_ID));
    }

    @Test
    public void factoryMethodShouldCreateInstance() {
        FollowUpStepMetadata meta = FollowUpStepMetadata.of(PLAN_ID, TEMPLATE_ID);
        assertEquals(PLAN_ID, meta.planId());
        assertEquals(TEMPLATE_ID, meta.templateId());
    }

    @Test
    public void toStringShouldContainKeyFields() {
        FollowUpStepMetadata meta = new FollowUpStepMetadata(PLAN_ID, TEMPLATE_ID);
        String str = meta.toString();
        assertTrue(str.contains("planId=" + PLAN_ID));
        assertTrue(str.contains("templateId=" + TEMPLATE_ID));
    }

    @Test
    public void equalsAndHashCodeShouldWork() {
        FollowUpStepMetadata meta1 = new FollowUpStepMetadata(PLAN_ID, TEMPLATE_ID);
        FollowUpStepMetadata meta2 = new FollowUpStepMetadata(PLAN_ID, TEMPLATE_ID);
        assertEquals(meta1, meta2);
        assertEquals(meta1.hashCode(), meta2.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentFields() {
        FollowUpStepMetadata meta1 = new FollowUpStepMetadata(PLAN_ID, TEMPLATE_ID);
        FollowUpStepMetadata meta2 = new FollowUpStepMetadata(PLAN_ID, null);
        assertNotEquals(meta1, meta2);
    }
}