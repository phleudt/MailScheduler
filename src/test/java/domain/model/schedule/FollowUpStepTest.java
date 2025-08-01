package domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FollowUpStepTest {
    EntityId<FollowUpStep> DEFAULT_ID = EntityId.of(1L);
    int DEFAULT_STEP_NUMBER = 1;
    int DEFAULT_WAIT_PERIOD = 4;

    private FollowUpStep.Builder defaultBuilder() {
        return new FollowUpStep.Builder()
                .id(DEFAULT_ID)
                .stepNumber(DEFAULT_STEP_NUMBER)
                .waitPeriod(DEFAULT_WAIT_PERIOD);
    }

    @Test
    public void shouldCreateValidFollowUpStep() {
        FollowUpStep followUpStep = defaultBuilder().build();

        assertNotNull(followUpStep);
        assertEquals(DEFAULT_ID, followUpStep.getId());
        assertEquals(DEFAULT_STEP_NUMBER, followUpStep.getStepNumber());
        assertEquals(DEFAULT_WAIT_PERIOD, followUpStep.getWaitPeriod());
    }

    @Test
    public void shouldCreateValidFollowUpStepWhenIdNull() {
        FollowUpStep followUpStep = defaultBuilder().id(null).build();

        assertNotNull(followUpStep);
        assertNull(followUpStep.getId());
        assertEquals(DEFAULT_STEP_NUMBER, followUpStep.getStepNumber());
        assertEquals(DEFAULT_WAIT_PERIOD, followUpStep.getWaitPeriod());
    }

    @Test
    public void isInitialStepShouldReturnTrueWhenStepNumberIsZero() {
        FollowUpStep followUpStep = defaultBuilder().stepNumber(0).build();
        assertTrue(followUpStep.isInitialStep());
    }

    @Test
    public void isInitialStepShouldReturnFalseWhenStepNumberIsNotNull() {
        FollowUpStep followUpStep = defaultBuilder().build();
        assertFalse(followUpStep.isInitialStep());
    }

    @Test
    public void equalsShouldReturnTrueWhenAllValuesAreEqual() {
        FollowUpStep followUpStep1 = defaultBuilder().build();
        FollowUpStep followUpStep2 = defaultBuilder().build();

        assertEquals(followUpStep1, followUpStep2);
    }

    @Test
    public void equalsShouldReturnFalseWhenAtLeastOneValuesAreDifferent() {
        FollowUpStep followUpStep1 = defaultBuilder().build();
        FollowUpStep followUpStep2 = defaultBuilder().stepNumber(DEFAULT_STEP_NUMBER + 1).build();

        assertNotEquals(followUpStep1, followUpStep2);
    }

    @Test
    public void equalsShouldReturnFalseWhenDifferentObjects() {
        assertNotEquals(new Object(), defaultBuilder().build());
    }
}
