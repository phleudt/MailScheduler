package domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import com.mailscheduler.domain.model.schedule.PlanStepWithTemplate;
import com.mailscheduler.domain.model.template.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlanStepWithTemplateTest {

    private FollowUpStep step(int number, int wait) {
        return new FollowUpStep(number, wait);
    }

    private Template template(long id) {
        Template t = mock(Template.class);
        when(t.getId()).thenReturn(EntityId.of(id));
        return t;
    }

    @Test
    public void shouldCreateWithStepAndTemplate() {
        FollowUpStep step = step(1, 5);
        Template template = template(10L);
        PlanStepWithTemplate planStep = new PlanStepWithTemplate(step, template);
        assertEquals(step, planStep.step());
        assertEquals(template, planStep.template());
    }

    @Test
    public void shouldThrowIfStepNull() {
        Template template = template(10L);
        assertThrows(NullPointerException.class, () -> new PlanStepWithTemplate(null, template));
    }

    @Test
    public void shouldThrowIfTemplateNull() {
        FollowUpStep step = step(1, 5);
        assertThrows(NullPointerException.class, () -> new PlanStepWithTemplate(step, null));
    }

    @Test
    public void getStepNumberAndWaitPeriodShouldDelegateToStep() {
        FollowUpStep step = step(2, 7);
        Template template = template(11L);
        PlanStepWithTemplate planStep = new PlanStepWithTemplate(step, template);
        assertEquals(2, planStep.getStepNumber());
        assertEquals(7, planStep.getWaitPeriod());
    }

    @Test
    public void isInitialStepShouldDelegateToStep() {
        FollowUpStep step = step(0, 3);
        Template template = template(12L);
        PlanStepWithTemplate planStep = new PlanStepWithTemplate(step, template);
        assertTrue(planStep.isInitialStep());
    }

    @Test
    public void equalsAndHashCodeShouldWork() {
        FollowUpStep step = step(1, 5);
        Template template = template(13L);
        PlanStepWithTemplate a = new PlanStepWithTemplate(step, template);
        PlanStepWithTemplate b = new PlanStepWithTemplate(step, template);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentFields() {
        FollowUpStep step1 = step(1, 5);
        FollowUpStep step2 = step(2, 5);
        Template template = template(14L);
        PlanStepWithTemplate a = new PlanStepWithTemplate(step1, template);
        PlanStepWithTemplate b = new PlanStepWithTemplate(step2, template);
        assertNotEquals(a, b);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        FollowUpStep step = step(3, 9);
        Template template = template(15L);
        PlanStepWithTemplate planStep = new PlanStepWithTemplate(step, template);
        String str = planStep.toString();
        assertTrue(str.contains("stepNumber=3"));
        assertTrue(str.contains("waitPeriod=9"));
    }
}