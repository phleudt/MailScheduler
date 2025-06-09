package com.mailscheduler.application.synchronization.template.gateway;

import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.email.Subject;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface GmailGateway {
    /**
     * Fetches all draft messages from Gmail.
     * @return List of draft messages with their IDs, subjects, and content
     * @throws IOException If there's an error communicating with Gmail
     */
     List<GmailDraft> listDrafts() throws IOException;

    /**
     * Gets a specific draft by its ID.
     * @param draftId The Gmail draft ID
     * @return The draft message if found
     * @throws IOException If there's an error communicating with Gmail
     */
    Optional<GmailDraft> getDraft(String draftId) throws IOException;

    /**
     * Represents a Gmail draft message.
     */
    record GmailDraft(
            String id,
            Subject subject,
            Body body
    ) {}
}
