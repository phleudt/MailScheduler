package com.mailscheduler.application.template;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.mailscheduler.common.exception.EmailTemplateManagerException;
import com.mailscheduler.common.exception.PlaceholderException;
import com.mailscheduler.domain.template.TemplateContent;
import com.mailscheduler.infrastructure.email.MimeMessageProcessor;
import com.mailscheduler.common.exception.validation.InvalidTemplateException;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.domain.template.Template;
import com.mailscheduler.domain.template.PlaceholderManager;
import com.mailscheduler.domain.template.TemplateCategory;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteTemplateRepository;
import com.mailscheduler.interfaces.cli.AbstractUserConsoleInteractionService;
import com.mailscheduler.common.util.TemplateUtils;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TemplateManager {
    private static final Logger LOGGER = Logger.getLogger(TemplateManager.class.getName());

    private final GmailService gmailService;
    private final SQLiteTemplateRepository templateRepository;
    private final TemplateSelectionService templateSelectionService;

    public TemplateManager(GmailService gmailService, SQLiteTemplateRepository templateRepository) {
        this.gmailService = gmailService;
        this.templateRepository = templateRepository;
        this.templateSelectionService = new TemplateSelectionService(gmailService);
    }

    public void manageTemplates(List<Draft> drafts, int numberOfFollowUps) throws EmailTemplateManagerException {
        try {
            if (!validateTemplateInitializationState()) return;

            if (drafts.isEmpty()) {
                LOGGER.warning("No drafts available for template management");
                return;
            }

            UserTemplateSelection selection = templateSelectionService.getUserTemplateSelection(drafts, numberOfFollowUps);

            Template initialTemplate = buildEmailTemplateFromDraft(
                    drafts.get(selection.initialTemplateIndex()),
                    TemplateCategory.DEFAULT_INITIAL,
                    0
            );

            List<Template> followUpTemplates = buildFollowUpTemplates(drafts, selection.followUpTemplateIndices());

            templateSelectionService.displaySelectedTemplates(initialTemplate, followUpTemplates);

            PlaceholderManager updatedPlaceholderManager = templateSelectionService.getUserPlaceholderReplacements(
                    initialTemplate, followUpTemplates
            );

            persistTemplatesWithPlaceholders(initialTemplate, followUpTemplates, updatedPlaceholderManager);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error during template management", e);
            throw new EmailTemplateManagerException("Failed to manage templates due to I/O error", e);
        } catch (InvalidTemplateException | PlaceholderException e) {
            LOGGER.log(Level.SEVERE, "Error processing email templates", e);
            throw new EmailTemplateManagerException("Invalid template or placeholder configuration", e);
        }
    }

    private boolean validateTemplateInitializationState() throws EmailTemplateManagerException {
        try {
            boolean initialTemplateExists = templateRepository.doesDefaultInitialTemplateExist();
            int numberOfFollowUpTemplates = templateRepository.countDefaultFollowUpTemplates();
            int numberOfSchedules = templateRepository.getScheduleCount(1);

            return !initialTemplateExists || numberOfFollowUpTemplates != numberOfSchedules;
        } catch (RepositoryException e) {
            throw new EmailTemplateManagerException("Failed to validate template initialization state");
        }
    }

    private List<Draft> fetchAvailableDrafts() throws IOException {
        return gmailService.getDrafts();
    }

    public Optional<Template> getDefaultInitialEmailTemplate() throws EmailTemplateManagerException {
        try {
            return templateRepository.findDefaultInitialEmailTemplate(); // TODO
        } catch (RepositoryException e) {
            LOGGER.log(Level.SEVERE, "Failed to get default initial email template", e);
            throw new EmailTemplateManagerException("Unable to retrieve default initial email template", e);
        }
    }

    public Optional<Template> getDefaultFollowUpEmailTemplate(int followupNumber) throws EmailTemplateManagerException{
        try {
            return templateRepository.findDefaultFollowUpEmailTemplateByNumber(followupNumber); // TODO
        } catch (RepositoryException e) {
            LOGGER.log(Level.SEVERE, "Failed to get default follow-up email template for follow-up number: " + followupNumber, e);
            throw new EmailTemplateManagerException("Unable to retrieve default follow-up email template", e);
        }
    }

    private List<Template> buildFollowUpTemplates(List<Draft> drafts, List<Integer> followUpTemplateIndices)
            throws IOException, InvalidTemplateException, PlaceholderException, EmailTemplateManagerException {
        List<Template> followUpTemplates = new ArrayList<>();
        for (int i = 0; i < followUpTemplateIndices.size(); i++) {
            int index = followUpTemplateIndices.get(i);
            Template template = buildEmailTemplateFromDraft(
                    drafts.get(index),
                    TemplateCategory.DEFAULT_FOLLOW_UP,
                    i + 1
            );
            followUpTemplates.add(template);
        }
        return followUpTemplates;
    }

    private void persistTemplatesWithPlaceholders(
            Template initialTemplate,
            List<Template> followUpTemplates,
            PlaceholderManager updatedPlaceholderManager
    ) throws PlaceholderException {
        initialTemplate.setPlaceholderManager(updatedPlaceholderManager);
        followUpTemplates.forEach(followUpTemplate -> followUpTemplate.setPlaceholderManager(updatedPlaceholderManager));

        try {
            templateRepository.save(initialTemplate);
            for (Template followUpTemplate : followUpTemplates) {
                templateRepository.save(followUpTemplate);
            }
        } catch (RepositoryException e) {
            LOGGER.log(Level.WARNING, "Failed to commit template transaction");
            System.out.println("Failed transaction for template");
            System.out.println(e.getMessage());
        }
    }

    private Template buildEmailTemplateFromDraft(Draft draft, TemplateCategory category, int followupNumber)
            throws IOException, InvalidTemplateException, PlaceholderException, EmailTemplateManagerException {
        if (TemplateCategory.DEFAULT_INITIAL.equals(category)) {
            try {
                if (templateRepository.doesDefaultInitialTemplateExist()) {
                    throw new EmailTemplateManagerException("A default initial email template already exists.");
                }
            } catch (RepositoryException e) {
                throw new EmailTemplateManagerException("Failed database operation for initial template", e);
            }
        } else if (TemplateCategory.DEFAULT_FOLLOW_UP.equals(category)) {
            try {
                if (templateRepository.doesDefaultFollowUpTemplateExist(followupNumber)) {
                    throw new EmailTemplateManagerException("A default follow-up email template already exists for follow-up number: " + followupNumber);
                }
            } catch (RepositoryException e) {
                throw new EmailTemplateManagerException("Failed database operation for follow-up template", e);
            }
        }

        Message message = gmailService.getDraftAsMessage(draft);

        // Process the MIME message to extract HTML and images
        MimeMessageProcessor processor = new MimeMessageProcessor();
        try {
            processor.processMessage(message);
        } catch (MessagingException e) {
            throw new EmailTemplateManagerException("Failed to process MIME message", e);
        }

        String subject = TemplateUtils.extractSubject(message);
        String body = processor.getHtmlContent(); // Use the HTML content from processor

        Set<String> subjectKeys = TemplateUtils.extractKeys(new char[] {'{', '}'}, subject);
        Set<String> bodyKeys = TemplateUtils.extractKeys(new char[] {'{', '}'}, body);

        Set<String> placeholderKeys = new HashSet<>(subjectKeys);
        placeholderKeys.addAll(bodyKeys);

        PlaceholderManager placeholderManager = new PlaceholderManager();
        for (String key : placeholderKeys) {
            placeholderManager.addStringPlaceholder(key, "NO PLACEHOLDER");
        }

        return new Template.Builder()
                .setCategory(category)
                .setSubject(subject)
                .setBody(body)
                .setPlaceholderManager(placeholderManager)
                .setFollowUpNumber(followupNumber)
                .setDraftId(draft.getId())
                .build();
    }

    public List<Draft> updateTemplatesFromDrafts() throws EmailTemplateManagerException {
        try {
            List<Draft> drafts = gmailService.getDrafts();
            for (Draft draft : drafts) {
                Message message = gmailService.getDraftAsMessage(draft);
                String draftId = draft.getId();

                Optional<Template> template = templateRepository.findByDraftId(draftId);


                if (template.isPresent()) {
                    Template template1 = template.get();

                    String newSubject = TemplateUtils.extractSubject(message);
                    String newBody = TemplateUtils.extractBody(message);
                    TemplateContent newContent = new TemplateContent(newSubject, newBody);

                    boolean hasContentChanged = !template1.getContent().equals(newContent);

                    if (hasContentChanged) {
                        boolean updateTemplate = templateSelectionService.promptUserForTemplateUpdate(template1, newSubject, newBody);

                        if (updateTemplate) {
                            template1.setContent(newContent);
                            template1.setDraftId(draftId);

                            templateRepository.update(template1);
                            System.out.println("Template updated successfully.");
                        } else {
                            System.out.println("Template update skipped.");
                        }
                    }
                }
            }

            return drafts;
        } catch (RepositoryException | IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to update templates from drafts", e);
            throw new EmailTemplateManagerException("Unable to update templates from drafts", e);
        }
    }

    public record UserTemplateSelection(int initialTemplateIndex, List<Integer> followUpTemplateIndices) {
    }
}

class TemplateSelectionService extends AbstractUserConsoleInteractionService {
    private final GmailService gmailService;

    public TemplateSelectionService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    public TemplateManager.UserTemplateSelection getUserTemplateSelection(List<Draft> drafts, int numberOfFollowUps) {
        displayDrafts(drafts);

        int initialTemplateIndex = getSingleTemplateIndex(drafts.size(), "Enter the number of the draft you want to use as the initial email templates:");
        List<Integer> followUpTemplateIndices = getFollowUpTemplateIndices(drafts.size(), numberOfFollowUps);

        return new TemplateManager.UserTemplateSelection(initialTemplateIndex, followUpTemplateIndices);
    }

    public boolean promptUserForTemplateUpdate(
            Template existingTemplate,
            String subject,
            String body
    ) {
        System.out.println("Template differences found:");
        System.out.println("Existing Subject: " + existingTemplate.getSubject());
        System.out.println("New Subject: " + subject);
        System.out.println("Existing Body: " + existingTemplate.getBody());
        System.out.println("New Body: " + body);

        System.out.println("Do you want to update the template? (yes/no): ");
        String userInput = scanner.nextLine();
        return "yes".equalsIgnoreCase(userInput) || "y".equalsIgnoreCase(userInput);
    }

    public PlaceholderManager getUserPlaceholderReplacements(Template initialTemplate, List<Template> followUpTemplates) {
        Set<String> placeholders = extractAllPlaceholders(initialTemplate, followUpTemplates);
        displayPlaceholders(placeholders);

        return getPlaceholderReplacements(placeholders);
    }

    private PlaceholderManager getPlaceholderReplacements(Set<String> placeholders) {
        PlaceholderManager placeholderManager = new PlaceholderManager();
        System.out.println("Setting up replacement strategy");

        int i = 1;
        for (String placeholder : placeholders) {
            System.out.println(i + ". Placeholder: " + placeholder);

            List<String> valueTypes = Arrays.stream(PlaceholderManager.ValueType.values())
                    .map(PlaceholderManager.ValueType::name)
                    .toList();

            System.out.println("Select the value type for the placeholder:");
            for (int j = 0; j < valueTypes.size(); j++) {
                System.out.println((j + 1) + ". " + valueTypes.get(j));
            }
            System.out.println("Only columns are currently supported for the SPREADSHEET_REFERENCE value type\n");

            int valueTypeChoice = getValidatedIntegerInput("Select value type:", valueTypes.size()) - 1;
            PlaceholderManager.ValueType valueType = PlaceholderManager.ValueType.valueOf(valueTypes.get(valueTypeChoice));

            System.out.println("Enter the replacement value for the placeholder:");
            scanner.nextLine();
            while (true) {
                try {
                    System.out.print("> ");
                    String replacementValue = scanner.nextLine().trim();

                    // Additional configuration based on value type
                    switch (valueType) {
                        case STRING -> {
                            placeholderManager.addStringPlaceholder(placeholder, replacementValue);
                        }
                        case SPREADSHEET_REFERENCE -> {
                            String uppercaseColumn = replacementValue.toUpperCase();

                            if (!SpreadsheetReference.isValidColumn(uppercaseColumn)) {
                                System.out.println("Invalid column. Please enter a valid column (A-Z):");
                                continue;
                            }

                            SpreadsheetReference reference = SpreadsheetReference.ofColumn(uppercaseColumn);
                            placeholderManager.addSpreadsheetPlaceholder(placeholder, reference);
                        }
                    }
                    break;
                } catch (PlaceholderException e) {
                    System.out.println("Error adding placeholder: " + e.getMessage());
                }
            }
            i++;
        }

        return placeholderManager;
    }

    private Set<String> extractAllPlaceholders(Template template, List<Template> templates) {
        Set<String> placeholders = new HashSet<>(template.getPlaceholderManager().getAllPlaceholderKeys());
        for (Template emailTemplate : templates) {
            placeholders.addAll(emailTemplate.getPlaceholderManager().getAllPlaceholderKeys());
        }
        return placeholders;
    }

    private void displayPlaceholders(Set<String> placeholders) {
        System.out.println("Available placeholders:");

        int count = 1;
        for (String placeholder : placeholders) {
            System.out.println(count + ". " + placeholder);
            count++;
        }
        System.out.println("\n");
    }

    private int getSingleTemplateIndex(int numberOfDrafts, String prompt) {
        return getValidatedIntegerInput(
                prompt,
                numberOfDrafts) - 1;
    }

    private List<Integer> getFollowUpTemplateIndices(int numberOfDrafts, int numberOfFollowUps) {
        while (true) {
            List<Integer> choices = getValidatedMultipleInputs(
                    "Enter the numbers of drafts you want to use as followup-templates (comma-seperated) (order matters):",
                    numberOfDrafts
            );

            if (choices.size() == numberOfFollowUps) {
                return choices.stream()
                        .map(s -> s - 1)
                        .toList();
            }
            System.out.println("Number of follow-up templates must equal: " + numberOfFollowUps);
        }
    }

    private void displayDrafts(List<Draft> drafts) {
        int count = 1;
        for (Draft draft : drafts) {
            // Fetch the full message details for each draft
            try {
                Message message = gmailService.getDraftAsMessage(draft);
                String subject = TemplateUtils.extractSubject(message);
                String snippet = message.getSnippet();
                System.out.println(count + ". \t" + "Subject: " + (subject.isEmpty() ? "EMPTY" : subject) + " - Snippet: " + snippet);
            } catch (IOException e) {
                System.err.println("Failed to get draft: " + draft.toString() + " as message");;
            }
            count++;
        }
    }

    public void displaySelectedTemplates(Template initialTemplate, List<Template> followUpTemplates) {
        System.out.println("Initial Template:");
        System.out.println("Subject: " + initialTemplate.getSubject());
        System.out.println("Body: " + initialTemplate.getBody());

        int count = 1;
        System.out.println("Follow-Up Templates: ");
        for (Template followUpTemplate : followUpTemplates) {
            System.out.println(count + ". FollowUp:");
            System.out.println("Subject: " + followUpTemplate.getSubject());
            System.out.println("Body: " + followUpTemplate.getBody());
            count++;
        }
    }

}
