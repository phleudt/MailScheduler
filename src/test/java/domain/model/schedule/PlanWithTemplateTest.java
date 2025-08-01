package domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import com.mailscheduler.domain.model.schedule.PlanStepWithTemplate;
import com.mailscheduler.domain.model.schedule.PlanWithTemplate;
import com.mailscheduler.domain.model.template.Template;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlanWithTemplateTest {

    private PlanStepWithTemplate stepWithTemplate(int stepNumber, int wait, long templateId) {
        FollowUpStep step = new FollowUpStep(stepNumber, wait);
        Template template = mock(Template.class);
        when(template.getId()).thenReturn(EntityId.of(templateId));
        return new PlanStepWithTemplate(step, template);
    }

    @Test
    public void shouldCreateEmptyPlan() {
        PlanWithTemplate plan = new PlanWithTemplate();
        assertTrue(plan.getStepsWithTemplates().isEmpty());
    }

    @Test
    public void shouldCreatePlanWithSteps() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1, s2));
        assertEquals(2, plan.getStepsWithTemplates().size());
        assertEquals(s1, plan.getStepsWithTemplates().get(0));
        assertEquals(s2, plan.getStepsWithTemplates().get(1));
    }

    @Test
    public void addStepShouldAddToList() {
        PlanWithTemplate plan = new PlanWithTemplate();
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        plan.addStep(s1);
        assertEquals(1, plan.getStepsWithTemplates().size());
        assertEquals(s1, plan.getStepsWithTemplates().get(0));
    }

    @Test
    public void getStepsWithTemplatesShouldReturnUnmodifiableList() {
        PlanWithTemplate plan = new PlanWithTemplate();
        assertThrows(UnsupportedOperationException.class, () -> plan.getStepsWithTemplates().add(stepWithTemplate(0, 1, 1L)));
    }

    @Test
    public void getNextStepShouldReturnCorrectStep() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1, s2));
        Optional<PlanStepWithTemplate> next = plan.getNextStep(0);
        assertTrue(next.isPresent());
        assertEquals(s2, next.get());
    }

    @Test
    public void getNextStepShouldReturnEmptyIfNoMoreSteps() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1));
        assertTrue(plan.getNextStep(0).isEmpty());
    }

    @Test
    public void getFollowUpCountShouldReturnCorrectValue() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1, s2));
        assertEquals(1, plan.getFollowUpCount());
    }

    @Test
    public void getFollowUpCountShouldReturnZeroIfOnlyInitialStep() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1));
        assertEquals(0, plan.getFollowUpCount());
    }

    @Test
    public void equalsAndHashCodeShouldWork() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate a = new PlanWithTemplate(List.of(s1, s2));
        PlanWithTemplate b = new PlanWithTemplate(List.of(s1, s2));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentSteps() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate a = new PlanWithTemplate(List.of(s1, s2));
        PlanWithTemplate b = new PlanWithTemplate(List.of(s1));
        assertNotEquals(a, b);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        PlanStepWithTemplate s1 = stepWithTemplate(0, 2, 1L);
        PlanStepWithTemplate s2 = stepWithTemplate(1, 4, 2L);
        PlanWithTemplate plan = new PlanWithTemplate(List.of(s1, s2));
        String str = plan.toString();
        assertTrue(str.contains("steps=2"));
    }
}