package domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpPlanType;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FollowUpPlanTest {

    private static final EntityId<FollowUpPlan> PLAN_ID = EntityId.of(1L);

    private FollowUpPlan.Builder defaultBuilder() {
        return new FollowUpPlan.Builder()
                .setId(PLAN_ID)
                .setFollowUpPlanType(FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN)
                .addStep(3)
                .addStep(5);
    }

    @Test
    public void shouldCreateValidFollowUpPlan() {
        FollowUpPlan plan = defaultBuilder().build();
        assertNotNull(plan);
        assertEquals(PLAN_ID, plan.getId());
        assertEquals(FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN, plan.getPlanType());
        assertEquals(2, plan.getSteps().size());
    }

    @Test
    public void getStepsShouldReturnUnmodifiableList() {
        FollowUpPlan plan = defaultBuilder().build();
        List<FollowUpStep> steps = plan.getSteps();
        assertThrows(UnsupportedOperationException.class, () -> steps.add(new FollowUpStep(2, 7)));
    }

    @Test
    public void getNextStepShouldReturnCorrectStep() {
        FollowUpPlan plan = defaultBuilder().build();
        Optional<FollowUpStep> next = plan.getNextStep(0);
        assertTrue(next.isPresent());
        assertEquals(1, next.get().getStepNumber());
        assertEquals(5, next.get().getWaitPeriod());
    }

    @Test
    public void getNextStepShouldReturnEmptyIfNoMoreSteps() {
        FollowUpPlan plan = defaultBuilder().build();
        Optional<FollowUpStep> next = plan.getNextStep(1);
        assertTrue(next.isEmpty());
    }

    @Test
    public void getStepByNumberShouldReturnCorrectStep() {
        FollowUpPlan plan = defaultBuilder().build();
        Optional<FollowUpStep> step = plan.getStepByNumber(1);
        assertTrue(step.isPresent());
        assertEquals(1, step.get().getStepNumber());
    }

    @Test
    public void getStepByNumberShouldReturnEmptyIfNotFound() {
        FollowUpPlan plan = defaultBuilder().build();
        assertTrue(plan.getStepByNumber(99).isEmpty());
    }

    @Test
    public void shouldSetAndReturnPlanType() {
        FollowUpPlan plan = new FollowUpPlan.Builder()
                .setId(PLAN_ID)
                .setFollowUpPlanType(FollowUpPlanType.FOLLOW_UP_PLAN)
                .addStep(2)
                .build();
        assertEquals(FollowUpPlanType.FOLLOW_UP_PLAN, plan.getPlanType());
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        FollowUpPlan plan1 = defaultBuilder().build();
        FollowUpPlan plan2 = defaultBuilder().build();
        assertEquals(plan1, plan2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentSteps() {
        FollowUpPlan plan1 = defaultBuilder().build();
        FollowUpPlan plan2 = new FollowUpPlan.Builder()
                .setId(PLAN_ID)
                .setFollowUpPlanType(FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN)
                .addStep(3)
                .build();
        assertNotEquals(plan1, plan2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentType() {
        FollowUpPlan plan1 = defaultBuilder().build();
        FollowUpPlan plan2 = new FollowUpPlan.Builder()
                .setId(PLAN_ID)
                .setFollowUpPlanType(FollowUpPlanType.FOLLOW_UP_PLAN)
                .addStep(3)
                .addStep(5)
                .build();
        assertNotEquals(plan1, plan2);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        FollowUpPlan plan = defaultBuilder().build();
        String str = plan.toString();
        assertTrue(str.contains("id=" + PLAN_ID));
        assertTrue(str.contains("type=" + FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN));
        assertTrue(str.contains("steps=2"));
    }
}