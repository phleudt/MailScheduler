package domain.model.schedule;

import com.mailscheduler.domain.model.schedule.FollowUpPlanType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FollowUpPlanTypeTest {

    @Test
    public void shouldCreateDefaultFollowUpPlanType() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN;
        assertEquals(followUpPlanType, FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN);
    }

    @Test
    public void shouldCreateFollowUpPlanType() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.FOLLOW_UP_PLAN;
        assertEquals(followUpPlanType, FollowUpPlanType.FOLLOW_UP_PLAN);
    }

    @Test
    public void isDefaultShouldReturnTrueWhenFollowUpPlanTypeIsDefault() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN;
        assertTrue(followUpPlanType.isDefault());
    }

    @Test
    public void isDefaultShouldReturnFalseWhenFollowUpPlanTypeIsNotDefault() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.FOLLOW_UP_PLAN;
        assertFalse(followUpPlanType.isDefault());
    }

    @Test
    public void isCustomShouldReturnTrueWhenFollowUpPlanTypeIsCustom() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.FOLLOW_UP_PLAN;
        assertTrue(followUpPlanType.isCustom());
    }

    @Test
    public void isCustomShouldReturnFalseWhenFollowUpPlanTypeIsNotCustom() {
        FollowUpPlanType followUpPlanType = FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN;
        assertFalse(followUpPlanType.isCustom());
    }
}
