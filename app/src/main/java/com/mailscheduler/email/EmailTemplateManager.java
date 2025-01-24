package com.mailscheduler.email;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.mailscheduler.database.dao.EmailTemplateDao;
import com.mailscheduler.database.entities.EmailTemplateEntity;
import com.mailscheduler.exception.*;
import com.mailscheduler.exception.dao.EmailTemplateDaoException;
import com.mailscheduler.exception.validation.InvalidTemplateException;
import com.mailscheduler.google.GmailService;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.EmailTemplate;
import com.mailscheduler.model.PlaceholderManager;
import com.mailscheduler.model.TemplateCategory;
import com.mailscheduler.service.AbstractUserConsoleInteractionService;
import com.mailscheduler.util.TemplateUtils;
import com.mailscheduler.model.SpreadsheetReference;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EmailTemplateManager {
    private static final Logger LOGGER = Logger.getLogger(EmailTemplateManager.class.getName());

    private final GmailService gmailService;
    private final EmailTemplateDao emailTemplateDao;
    private final TemplateSelectionService templateSelectionService;

    public EmailTemplateManager(GmailService gmailService, EmailTemplateDao emailTemplateDao) {
        this.gmailService = gmailService;
        this.emailTemplateDao = emailTemplateDao;
        this.templateSelectionService = new TemplateSelectionService(gmailService);
    }


    /**
     * Manages email templates by importing from Gmail drafts.
     *
     * @throws EmailTemplateManagerException If there are issues during template management
     */
    public void manageTemplates(List<Draft> drafts, int numberOfFollowUps) throws EmailTemplateManagerException {
        try {
            if (!validateTemplateInitializationState()) return;

            if (drafts.isEmpty()) {
                LOGGER.warning("No drafts available for template management");
                return;
            }

            UserTemplateSelection selection = templateSelectionService.getUserTemplateSelection(drafts, numberOfFollowUps);

            EmailTemplate initialTemplate = buildEmailTemplateFromDraft(
                    drafts.get(selection.getInitialTemplateIndex()),
                    TemplateCategory.DEFAULT_INITIAL,
                    0
            );

            List<EmailTemplate> followUpTemplates = buildFollowUpTemplates(drafts, selection.getFollowUpTemplateIndices());

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
            boolean initialTemplateExists = emailTemplateDao.doesDefaultInitialTemplateExist();
            int numberOfFollowUpTemplates = emailTemplateDao.countDefaultFollowUpTemplates();
            int numberOfSchedules = emailTemplateDao.getScheduleCount(1);

            return !initialTemplateExists || numberOfFollowUpTemplates != numberOfSchedules;
        } catch (EmailTemplateDaoException e) {
            throw new EmailTemplateManagerException("Failed to validate template initialization state", e);
        }
    }

    private List<Draft> fetchAvailableDrafts() throws IOException {
        return gmailService.getDrafts();
    }

    /**
     * Retrieves the default initial email template.
     *
     * @return The default initial EmailTemplate
     * @throws EmailTemplateManagerException If unable to retrieve the template
     */
    public EmailTemplate getDefaultInitialEmailTemplate() throws EmailTemplateManagerException {
        try {
            return EntityMapper.toEmailTemplate(emailTemplateDao.findDefaultInitialEmailTemplate());
        } catch (EmailTemplateDaoException.NotFound e) {
            LOGGER.log(Level.SEVERE, "Default initial email template not found", e);
            throw new EmailTemplateManagerException("Default initial email template not found", e);
        } catch (EmailTemplateDaoException | MappingException e) {
            LOGGER.log(Level.SEVERE, "Failed to get default initial email template", e);
            throw new EmailTemplateManagerException("Unable to retrieve default initial email template", e);
        }
    }

    /**
     * Retrieves the default follow-up email template for a specific follow-up number.
     *
     * @param followupNumber The follow-up number
     * @return The default follow-up EmailTemplate
     * @throws EmailTemplateManagerException If unable to retrieve the template
     */
    public EmailTemplate getDefaultFollowUpEmailTemplate(int followupNumber) throws EmailTemplateManagerException{
        try {
            return EntityMapper.toEmailTemplate(emailTemplateDao.findDefaultFollowUpEmailTemplateByNumber(followupNumber));
        } catch (EmailTemplateDaoException | MappingException e) {
            LOGGER.log(Level.SEVERE, "Failed to get default follow-up email template for follow-up number: " + followupNumber, e);
            throw new EmailTemplateManagerException("Unable to retrieve default follow-up email template", e);
        }
    }

    // public void importTemplateFromFile(String file);

    private List<EmailTemplate> buildFollowUpTemplates(List<Draft> drafts, List<Integer> followUpTemplateIndices)
            throws IOException, InvalidTemplateException, PlaceholderException, EmailTemplateManagerException {
        List<EmailTemplate> followUpTemplates = new ArrayList<>();
        for (int i = 0; i < followUpTemplateIndices.size(); i++) {
            int index = followUpTemplateIndices.get(i);
            EmailTemplate emailTemplate = buildEmailTemplateFromDraft(
                    drafts.get(index),
                    TemplateCategory.DEFAULT_FOLLOW_UP,
                    i + 1
            );
            followUpTemplates.add(emailTemplate);
        }
        return followUpTemplates;
    }


    /**
     * Updates templates with placeholder replacements.
     *
     * @param initialTemplate The initial email template
     * @param followUpTemplates List of follow-up email templates
     * @param updatedPlaceholderManager Map of placeholder replacements
     * @throws PlaceholderException If placeholder management fails
     * @throws EmailTemplateManagerException If template saving fails
     */
    private void persistTemplatesWithPlaceholders(
            EmailTemplate initialTemplate,
            List<EmailTemplate> followUpTemplates,
            PlaceholderManager updatedPlaceholderManager
    ) throws PlaceholderException, EmailTemplateManagerException {
        initialTemplate.setPlaceholderManager(updatedPlaceholderManager);
        followUpTemplates.forEach(followUpTemplate -> followUpTemplate.setPlaceholderManager(updatedPlaceholderManager));

        try {
            try {
                emailTemplateDao.beginTransaction();
                saveTemplate(initialTemplate);
                for (EmailTemplate followUpTemplate : followUpTemplates) {
                    saveTemplate(followUpTemplate);
                }
                emailTemplateDao.commitTransaction();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to commit template transaction");
                System.out.println("Failed transaction for template");
                System.out.println(e.getMessage());
            }
        } catch (EmailTemplateDaoException | MappingException e) {
            LOGGER.log(Level.SEVERE, "Failed to save email templates", e);
            throw new EmailTemplateManagerException("Unable to save email templates", e);
        }
    }

    private void saveTemplate(EmailTemplate emailTemplate) throws EmailTemplateDaoException, MappingException {
        emailTemplateDao.insertEmailTemplate(EntityMapper.toEmailTemplateEntity(emailTemplate));
    }

    /**
     * Builds an email template from a Gmail draft.
     *
     * @param draft The Gmail draft
     * @param templateCategory The template category
     * @param followupNumber The follow-up number
     * @return The built EmailTemplate
     * @throws IOException If there's an issue retrieving draft details
     * @throws InvalidTemplateException If the template is invalid
     * @throws PlaceholderException If placeholder management fails
     */
    private EmailTemplate buildEmailTemplateFromDraft(Draft draft, TemplateCategory templateCategory, int followupNumber)
            throws IOException, InvalidTemplateException, PlaceholderException, EmailTemplateManagerException {
        if (TemplateCategory.DEFAULT_INITIAL.equals(templateCategory)) {
            try {
                if (emailTemplateDao.doesDefaultInitialTemplateExist()) {
                    throw new EmailTemplateManagerException("A default initial email template already exists.");
                }
            } catch (EmailTemplateDaoException e) {
                throw new EmailTemplateManagerException("Failed database operation for initial template", e);
            }
        } else if (TemplateCategory.DEFAULT_FOLLOW_UP.equals(templateCategory)) {
            try {
                if (emailTemplateDao.doesDefaultFollowUpTemplateExist(followupNumber)) {
                    throw new EmailTemplateManagerException("A default follow-up email template already exists for follow-up number: " + followupNumber);
                }
            } catch (EmailTemplateDaoException e) {
                throw new EmailTemplateManagerException("Failed database operation for follow-up template", e);
            }
        }

        Message message = gmailService.getDraftAsMessage(draft);
        String subject = TemplateUtils.extractSubject(message);
        String body = TemplateUtils.extractBody(message);

        Set<String> subjectKeys = TemplateUtils.extractKeys(new char[] {'{', '}'}, subject);
        Set<String> bodyKeys = TemplateUtils.extractKeys(new char[] {'{', '}'}, body);

        Set<String> placeholderKeys = new HashSet<>(subjectKeys);
        placeholderKeys.addAll(bodyKeys);

        PlaceholderManager placeholderManager = new PlaceholderManager();
        for (String key : placeholderKeys) {
            placeholderManager.addStringPlaceholder(key, "NO PLACEHOLDER");
        }

        return new EmailTemplate.Builder()
                .setTemplateCategory(templateCategory)
                .setSubjectTemplate(subject)
                .setBodyTemplate(body)
                .setDefaultPlaceholderSymbols()
                .setPlaceholderManager(placeholderManager)
                .setFollowupNumber(followupNumber)
                .setDraftId(draft.getId())
                .build();
    }

    /**
     * Updates email templates from Gmail drafts.
     *
     * @throws EmailTemplateManagerException If there are issues updating templates
     */
    public List<Draft> updateTemplatesFromDrafts() throws EmailTemplateManagerException {
        try {
            List<Draft> drafts = gmailService.getDrafts();
            for (Draft draft : drafts) {
                Message message = gmailService.getDraftAsMessage(draft);
                String draftId = draft.getId();

                EmailTemplateEntity templateEntity = emailTemplateDao.findByDraftId(draftId);


                if (templateEntity != null) {
                    EmailTemplate existingTemplate = EntityMapper.toEmailTemplate(templateEntity);
                    String subject = TemplateUtils.extractSubject(message);
                    String body = TemplateUtils.extractBody(message);

                    boolean isSubjectDifferent = !existingTemplate.getSubjectTemplate().equals(subject);
                    boolean isBodyDifferent = !existingTemplate.getBodyTemplate().equals(body);

                    if (isSubjectDifferent || isBodyDifferent) {
                        boolean updateTemplate = templateSelectionService.promptUserForTemplateUpdate(existingTemplate, subject, body);

                        if (updateTemplate) {
                            existingTemplate.setSubjectTemplate(subject);
                            existingTemplate.setBodyTemplate(body);

                            emailTemplateDao.updateEmailTemplateById(
                                    existingTemplate.getId(),
                                    EntityMapper.toEmailTemplateEntity(existingTemplate)
                            );
                            System.out.println("Template updated successfully.");
                        } else {
                            System.out.println("Template update skipped.");
                        }
                    }
                }
            }

            return drafts;
        } catch (IOException | EmailTemplateDaoException | MappingException e) {
            LOGGER.log(Level.SEVERE, "Failed to update templates from drafts", e);
            throw new EmailTemplateManagerException("Unable to update templates from drafts", e);
        }
    }


    public static class UserTemplateSelection {
        private final int initialTemplateIndex;
        private final List<Integer> followUpTemplateIndices;

        public UserTemplateSelection(int initialTemplateIndex, List<Integer> followUpTemplateIndices) {
            this.initialTemplateIndex = initialTemplateIndex;
            this.followUpTemplateIndices = followUpTemplateIndices;
        }

        public int getInitialTemplateIndex() {
            return initialTemplateIndex;
        }

        public List<Integer> getFollowUpTemplateIndices() {
            return followUpTemplateIndices;
        }
    }
}

class TemplateSelectionService extends AbstractUserConsoleInteractionService {
    private final GmailService gmailService;

    public TemplateSelectionService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    public EmailTemplateManager.UserTemplateSelection getUserTemplateSelection(List<Draft> drafts, int numberOfFollowUps) {
        displayDrafts(drafts);

        int initialTemplateIndex = getInitialTemplateIndex(drafts.size());
        List<Integer> followUpTemplateIndices = getFollowUpTemplateIndices(drafts.size(), numberOfFollowUps);

        return new EmailTemplateManager.UserTemplateSelection(initialTemplateIndex, followUpTemplateIndices);
    }

    public boolean promptUserForTemplateUpdate(
            EmailTemplate existingTemplate,
            String subject,
            String body
    ) {
        System.out.println("Template differences found:");
        System.out.println("Existing Subject: " + existingTemplate.getSubjectTemplate());
        System.out.println("New Subject: " + subject);
        System.out.println("Existing Body: " + existingTemplate.getBodyTemplate());
        System.out.println("New Body: " + body);

        System.out.println("Do you want to update the template? (yes/no): ");
        String userInput = scanner.nextLine();
        return "yes".equalsIgnoreCase(userInput);
    }

    public PlaceholderManager getUserPlaceholderReplacements(EmailTemplate initialTemplate, List<EmailTemplate> followUpTemplates) {
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

    private Set<String> extractAllPlaceholders(EmailTemplate template, List<EmailTemplate> emailTemplates) {
        Set<String> placeholders = new HashSet<>(template.getPlaceholderManager().getAllPlaceholderKeys());
        for (EmailTemplate emailTemplate : emailTemplates) {
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

    private int getInitialTemplateIndex(int numberOfDrafts) {
        return getValidatedIntegerInput(
                "Enter the number of the draft you want to use as the initial email templates:",
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

    public void displaySelectedTemplates(EmailTemplate initialTemplate, List<EmailTemplate> followUpTemplates) {
        System.out.println("Initial Template:");
        System.out.println("Subject: " + initialTemplate.getSubjectTemplate());
        System.out.println("Body: " + initialTemplate.getSubjectTemplate());

        int count = 1;
        System.out.println("Follow-Up Templates: ");
        for (EmailTemplate followUpTemplate : followUpTemplates) {
            System.out.println(count + ". FollowUp:");
            System.out.println("Subject: " + followUpTemplate.getSubjectTemplate());
            System.out.println("Body: " + followUpTemplate.getBodyTemplate());
            count++;
        }
    }

}
