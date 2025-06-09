package com.mailscheduler.domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.template.Template;

import java.util.Objects;

/**
 * Metadata associated with a follow-up step.
 * <p>
 *    This record contains persistence-specific data related to a follow-up step, including references
 *    to its parent plan and associated template. It implements the EntityMetadata interface
 *    to support the repository pattern.
 * </p>
 */
public record FollowUpStepMetadata(
        EntityId<FollowUpPlan> planId,
        EntityId<Template> templateId
) implements EntityMetadata {

    /**
     * Creates validated follow-up step metadata.
     *
     * @throws NullPointerException if planId is null
     */
    public FollowUpStepMetadata {
        Objects.requireNonNull(planId, "Plan ID cannot be null");
        // Template ID can be null in some cases
    }

    /**
     * Factory method to create a new FollowUpStepMetadata.
     *
     * @param planId The ID of the parent follow-up plan
     * @param templateId The ID of the associated template (can be null)
     * @return A new FollowUpStepMetadata instance
     */
    public static FollowUpStepMetadata of(EntityId<FollowUpPlan> planId, EntityId<Template> templateId) {
        return new FollowUpStepMetadata(planId, templateId);
    }

    @Override
    public String toString() {
        return "FollowUpStepMetadata{" +
                "planId=" + planId +
                ", templateId=" + templateId +
                "}";
    }
}
