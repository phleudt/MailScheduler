package com.mailscheduler.application.email.sending.gateway;

import com.mailscheduler.application.email.sending.EmailSendException;
import com.mailscheduler.application.email.sending.SendResult;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.common.vo.ThreadId;

import java.io.IOException;

public interface EmailGateway {
    /**
     * Sends an email.
     *
     * @param email The email to send
     * @param threadId The thread ID for reply tracking (can be null for initial emails)
     * @return The result of the send operation
     * @throws EmailSendException If sending fails
     */
    SendResult send(Email email, ThreadId threadId) throws EmailSendException;

    /**
     * Saves an email as a draft.
     *
     * @param email The email to save as draft
     * @param threadId The thread ID for reply tracking (can be null for initial emails)
     * @return The result of the draft operation
     * @throws EmailSendException If saving fails
     */
    SendResult saveDraft(Email email, ThreadId threadId) throws EmailSendException;

    /**
     * Checks if a thread has received replies.
     *
     * @param threadId The thread ID to check
     * @param minimumReplyCount The minimum number of replies to check for
     * @return true if there are at least minimumReplyCount replies, false otherwise
     * @throws IOException If checking fails
     */
    boolean hasReplies(ThreadId threadId, int minimumReplyCount) throws IOException;
}
