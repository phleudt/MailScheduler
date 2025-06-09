package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;

import java.util.List;

/**
 * Repository interface for managing Recipient entities.
 * <p>
 *     Recipients represent contacts that are actively part of an email sequence, with tracking
 *     for email delivery status, replies, and follow-up progress.
 * </p>
 */
public interface RecipientRepository extends Repository<Recipient, RecipientMetadata> {
    /**
     * Finds recipients associated with a specific contact ID.
     *
     * @param contactId The ID of the Contact.
     * @return A list of Recipient entities linked to the contact (often just one).
     */
    List<EntityData<Recipient, RecipientMetadata>> findByContactId(EntityId<Contact> contactId);

    /**
     * Finds all recipients currently assigned to a specific follow-up plan.
     *
     * @param planId The ID of the FollowUpPlan.
     * @return A list of Recipient entities assigned to the plan.
     */
    List<EntityData<Recipient, RecipientMetadata>> findByFollowUpPlanId(EntityId<FollowUpPlan> planId);

    /**
     * Finds recipients based on their reply status.
     *
     * @param hasReplied The reply status to filter by (true or false).
     * @return A list of recipients matching the reply status.
     */
    List<EntityData<Recipient, RecipientMetadata>> findByHasReplied(boolean hasReplied);

    /**
     * Finds recipients who haven't replied yet and do not have an initial contact date set.
     * This identifies recipients potentially ready for their first email in a sequence.
     * Note: Further service-level checks might be needed (e.g., existence of external emails).
     *
     * @return A list of recipients potentially needing initial contact based on these criteria.
     */
    List<EntityData<Recipient, RecipientMetadata>> findRecipientsNeedingInitialContact();

}