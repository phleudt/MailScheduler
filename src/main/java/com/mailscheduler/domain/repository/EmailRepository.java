package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.*;
import com.mailscheduler.domain.model.recipient.Recipient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Email entities.
 * <p>
 *     Emails represent both sent and scheduled messages in the system,
 *     with tracking for status, delivery dates, and relationship to recipients.
 * </p>
 */
public interface EmailRepository extends Repository<Email, EmailMetadata> {

    /**
     * Finds all emails whose status is PENDING and whose scheduledDate is on or before the given cutoff date.
     *
     * @param cutoff The date cutoff for finding scheduled emails
     * @return A list of emails scheduled to be sent on or before the cutoff date
     */
    List<EntityData<Email, EmailMetadata>> findPendingScheduledBefore(LocalDate cutoff);

    /**
     * Finds all emails in a given status.
     */
    List<EntityData<Email, EmailMetadata>> findByStatus(EmailStatus status);

    /**
     * Finds all emails of a given type.
     */
    List<EntityData<Email, EmailMetadata>> findByType(EmailType type);

    /**
     * Finds all emails associated with a specific recipient via its ID.
     *
     * @param recipientId The ID of the recipient.
     * @return A list of emails sent to or scheduled for the recipient.
     */
    List<EntityData<Email, EmailMetadata>> findByRecipientId(EntityId<Recipient> recipientId);

    /**
     * Finds the first (initial) email sent to a recipient.
     */
    Optional<EntityData<Email, EmailMetadata>> findInitialEmailByRecipientId(EntityId<Recipient> recipientId);
}
