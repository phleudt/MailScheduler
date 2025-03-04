package com.mailscheduler.infrastructure.google.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import com.mailscheduler.infrastructure.google.auth.GoogleAuthService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GmailService extends GoogleAuthService<Gmail> {
    private static volatile GmailService instance;
    private Gmail gmailService;

    private GmailService() throws Exception {
        super();
        if (instance != null) {
            throw new IllegalStateException("GmailService instance already exists");
        }
    }

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

    @Override
    protected List<String> getScopes() {
        return Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    }

    @Override
    protected void initializeService(Credential credential) {
        this.gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void readEmails() throws IOException {
        ListMessagesResponse messagesResponse = gmailService.users().messages().list("me").execute();
        List<Message> messages = messagesResponse.getMessages();

        if (messages == null || messages.isEmpty()) {
            System.out.println("No messages found");
        } else {
            System.out.println("Messages:");
            for (Message message : messages) {
                System.out.println(message.getId());
                // Get message details
                Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
                System.out.println("Message snippet: " + fullMessage.getSnippet());
            }
        }
    }

    public boolean hasReplies(String threadId, int minimumReplyCount) throws IOException {
        Thread thread = gmailService.users().threads().get("me", threadId).execute();
        return thread.getMessages() != null && thread.getMessages().size() > minimumReplyCount;
    }

    public Draft getDraft(String draftId) throws IOException {
        return gmailService.users().drafts().get("me", draftId).execute();
    }

    public List<Draft> getDrafts() throws IOException {
        ListDraftsResponse listDraftsResponse = gmailService.users().drafts().list("me").execute();
        return listDraftsResponse.getDrafts();
    }

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

    public Message sendEmail(Message message) throws IOException {
        return gmailService.users().messages().send("me", message).execute();
    }

    public Draft createDraft(Message message) throws IOException {
        Draft draft = new Draft();
        draft.setMessage(message);
        return gmailService.users().drafts().create("me", draft).execute();
    }
}
