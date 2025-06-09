package com.mailscheduler.infrastructure.google.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import com.mailscheduler.infrastructure.google.auth.GoogleAuthService;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for interacting with Gmail API.
 * Provides methods for sending emails, managing drafts, and checking message threads.
 */
public class GmailService extends GoogleAuthService<Gmail> {
    private static final Logger LOGGER = Logger.getLogger(GmailService.class.getName());
    private static volatile GmailService instance;
    private Gmail gmailService;

    /**
     * Private constructor for singleton pattern.
     *
     * @throws Exception If initialization fails
     */
    private GmailService() throws Exception {
        super();
        if (instance != null) {
            throw new IllegalStateException("GmailService instance already exists");
        }
    }

    /**
     * Gets the singleton instance of GmailService.
     *
     * @return The GmailService instance
     * @throws Exception If initialization fails
     */
    public static GmailService getInstance() throws Exception {
        if (instance == null) {
            synchronized (GmailService.class) {
                if (instance == null) {
                    instance = new GmailService();
                }
            }
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * Returns the required OAuth2 scopes for Gmail API.
     */
    @Override
    protected List<String> getScopes() {
        return Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    }

    /**
     * {@inheritDoc}
     * Initializes the Gmail service with the provided credential.
     */
    @Override
    protected void initializeService(Credential credential) {
        this.gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Reads and outputs emails from the authenticated user's inbox.
     *
     * @throws IOException If an I/O error occurs
     */
    public void readEmails() throws IOException {
        ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
        List<Message> messages = messagesResponse.getMessages();

        if (messages == null || messages.isEmpty()) {
            LOGGER.info("No messages found in the inbox");
        } else {
            LOGGER.info("Found " + messages.size() + " messages");
            for (Message message : messages) {
                // Get message details
                Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
                LOGGER.info("Message ID: " + message.getId() + ", Snippet: " + fullMessage.getSnippet());
            }
        }
    }

    /**
     * Checks if a thread has at least the specified number of replies.
     *
     * @param threadId The ID of the thread to check
     * @param minimumReplyCount The minimum number of replies to check for
     * @return true if the thread has at least minimumReplyCount messages, false otherwise
     * @throws IOException If an I/O error occurs
     */
    public boolean hasReplies(String threadId, int minimumReplyCount) throws IOException {
        Thread thread = gmailService.users().threads().get("me", threadId).execute();
        return thread.getMessages() != null && thread.getMessages().size() > minimumReplyCount;
    }

    /**
     * Gets a draft by its ID.
     *
     * @param draftId The ID of the draft to retrieve
     * @return The Draft object
     * @throws IOException If an I/O error occurs
     */
    public Draft getDraft(String draftId) throws IOException {
        return gmailService.users().drafts().get("me", draftId).execute();
    }

    /**
     * Retrieves all drafts and populates each draft with its full message content.
     * Uses batch requests for efficiency.
     *
     * @return List of Draft objects with fully populated messages
     * @throws IOException If an I/O error occurs
     */
    public List<Draft> getDraftsWithFullMessages() throws IOException {
        ListDraftsResponse listDraftsResponse = gmailService.users().drafts().list("me").execute();
        List<Draft> drafts = listDraftsResponse.getDrafts();

        if (drafts == null || drafts.isEmpty()) {
            LOGGER.info("No drafts found");
            return Collections.emptyList();
        }

        // Map the associate message ID with its original Draft object
        final Map<String, Draft> messageIdToDraftMap = new HashMap<>();

        BatchRequest batch = gmailService.batch();

        // Define callback for each message fetch
        JsonBatchCallback<Message> callback = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                LOGGER.log(Level.WARNING, "Error fetching message: {0}", e.getMessage());
            }

            @Override
            public void onSuccess(Message fetchedMessage, HttpHeaders responseHeaders) {
                if (fetchedMessage != null && fetchedMessage.getId() != null) {
                    Draft originalDraft = messageIdToDraftMap.get(fetchedMessage.getId());
                    if (originalDraft != null) {
                        // Set the fully fetched Message object onto the original Draft
                        originalDraft.setMessage(fetchedMessage);
                        LOGGER.fine("Populated message for draft with message ID: " + fetchedMessage.getId());
                    } else {
                        LOGGER.warning("Could not find original draft for message ID: " + fetchedMessage.getId());
                    }
                }
            }
        };

        int batchCount = 0;
        for (Draft draft : drafts) {
            if (draft.getMessage() != null && draft.getMessage().getId() != null) {
                String messageId = draft.getMessage().getId();
                messageIdToDraftMap.put(messageId, draft);

                gmailService.users().messages()
                        .get("me", messageId)
                        .setFormat("RAW")
                        .queue(batch, callback);
                batchCount++;
            }
        }

        if (batchCount > 0) {
            LOGGER.info("Executing batch request to fetch " + batchCount + " draft messages");
            batch.execute();
        }

        return drafts;

    }

    /**
     * Gets a draft's message with full content.
     *
     * @param draft The Draft object
     * @return The Message object with full content
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException If draft is null
     */
    public Message getDraftAsMessage(Draft draft) throws IOException {
        if (draft == null) {
            throw new IllegalArgumentException("Draft cannot be null");
        }

        String messageId = draft.getMessage().getId();
        return gmailService.users().messages()
                .get("me", messageId)
                .setFormat("raw")
                .execute();
    }

    /**
     * Sends an email.
     *
     * @param message The Message object to send
     * @return The sent Message
     * @throws IOException If an I/O error occurs
     */
    public Message sendEmail(Message message) throws IOException {
        LOGGER.info("Sending email");
        return gmailService.users().messages().send("me", message).execute();
    }

    /**
     * Creates a draft email.
     *
     * @param message The Message object to save as draft
     * @return The created Draft
     * @throws IOException If an I/O error occurs
     */
    public Draft createDraft(Message message) throws IOException {
        LOGGER.info("Creating draft email");
        Draft draft = new Draft();
        draft.setMessage(message);
        return gmailService.users().drafts().create("me", draft).execute();
    }
}
