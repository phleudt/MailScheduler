package com.mailscheduler.application.synchronization.template.gateway;

import com.google.api.services.gmail.model.Draft;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.util.TemplateUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GmailDraftAdapter implements GmailGateway {
    private final GmailService gmailService;

    public GmailDraftAdapter(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @Override
    public List<GmailDraft> listDrafts() throws IOException {
        List<Draft> drafts = gmailService.getDraftsWithFullMessages();

        List<GmailDraft> gmailDrafts = new ArrayList<>();

        for (Draft draft : drafts) {
            if (draft == null || draft.getMessage() == null) {
                continue; // Skip drafts that are null or have no message
            }

            if (!TemplateUtil.extractRecipient(draft.getMessage()).isEmpty()) {
                continue; // Skip drafts that have a recipient
            }

            gmailDrafts.add(new GmailDraft(
                    draft.getId(),
                    TemplateUtil.extractSubject(draft.getMessage()),
                    TemplateUtil.extractBody(draft.getMessage())
            ));
        }
        return gmailDrafts;
    }

    @Override
    public Optional<GmailDraft> getDraft(String draftId) throws IOException {
        Draft draft = gmailService.getDraft(draftId);

        if (draft != null) {
            return Optional.of(new GmailDraft(
                    draft.getId(),
                    TemplateUtil.extractSubject(draft.getMessage()),
                    TemplateUtil.extractBody(draft.getMessage())
            ));
        }
        return Optional.empty();
    }
}
