package com.mailscheduler.interfaces.cli;

import com.google.api.services.gmail.model.Draft;
import com.mailscheduler.application.email.EmailService;
import com.mailscheduler.application.recipient.RecipientSynchronizationService;
import com.mailscheduler.application.schedule.ScheduleService;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;
import com.mailscheduler.common.config.Configuration;
import com.mailscheduler.common.config.ConfigurationService;
import com.mailscheduler.domain.email.EmailStatus;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.application.template.TemplateManager;
import com.mailscheduler.common.exception.service.RecipientServiceException;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.infrastructure.google.sheet.GoogleSheetService;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    // Application components
    private Configuration configuration;
    private DatabaseManager databaseManager;
    private EmailService emailService;
    private RecipientSynchronizationService recipientSynchronizationService;
    private ScheduleService scheduleService;
    private TemplateManager templateManager;
    private ConfigurationService configurationService;

    private static final Map<String, String> CLI_OPTIONS = Map.of(
            "--reset", "Reset the entire project configuration and data",
            "-r", "Reset the entire project configuration and data",
            "--clear-database", "Clears every entry in the database and deletes all tables",
            "-c", "Clears every entry in the database and deletes all tables",
            "--modify-config", "Modify the existing configuration",
            "-m", "Modify the existing configuration",
            "--help", "Display help information",
            "-h", "Display help information",
            "--sync", "Synchronize application data without sending emails",
            "--dry-run", "Simulate email processing without sending emails"
    );


    public Main() throws Exception {
        initialize();
    }

    private void initialize() throws Exception {
        // Initialize configuration
        Path configPath = Path.of("./config.properties");
        this.configurationService = new ConfigurationService(configPath);
        this.configuration = configurationService.loadConfiguration();

        assert this.configuration != null;

        configurationService.saveConfiguration(this.configuration);

        // Initialize components
        this.databaseManager = DatabaseManager.getInstance();
        this.databaseManager.setupDatabase();

        this.emailService = new EmailService(
                GmailService.getInstance(),
                new SpreadsheetService(GoogleSheetService.getInstance(), configuration),
                configuration,
                new SQLiteTemplateRepository(databaseManager)
        );

        this.recipientSynchronizationService = new RecipientSynchronizationService(
                new SpreadsheetService(GoogleSheetService.getInstance(), configuration),
                configuration
        );

        this.scheduleService = new ScheduleService();
        this.templateManager = new TemplateManager(
                GmailService.getInstance(),
                new SQLiteTemplateRepository(databaseManager)
        );
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            synchronizeApplicationData();
            processEmails();
        } else handleCommandLineArguments(args);
    }

    private void handleCommandLineArguments(String[] args) throws Exception {
        for (String arg : args) {
            switch (arg) {
                case "--reset", "-r" -> resetProject();
                case "--clear-database", "-c" -> clearDatabase();
                case "--modify-config", "-m" -> modifyConfiguration();
                case "--help", "-h" -> displayHelp();
                case "--sync", "-s" -> synchronizeData();
                case "--dry-run", "-d" -> simulateEmailProcessing();
                default -> {
                    LOGGER.warn("Unknown argument: {}", arg);
                    displayHelp();
                }
            }
        }
    }

    private void resetProject() throws Exception {
        LOGGER.info("Initiating full project reset");
        clearDatabase();
        configurationService.deleteConfiguration();
        resetGoogleCredentials();
        LOGGER.info("Project reset completed. Please restart the application.");
    }

    private void modifyConfiguration() throws Exception {
        LOGGER.info("Starting configuration modification wizard");
        Configuration updatedConfig = configurationService.modifyConfiguration(configuration);
        configurationService.saveConfiguration(updatedConfig);
        LOGGER.info("Configuration updated successfully");
    }

    private void displayHelp() {
        System.out.println("Mail Scheduler Application");
        System.out.println("Available Command-Line Options:");
        CLI_OPTIONS.forEach((option, description) ->
                System.out.printf("  %-20s %s%n", option, description)
        );
    }

    private void synchronizeData() throws Exception {
        LOGGER.info("Starting data synchronization");
        recipientSynchronizationService.syncRecipients();
        List<Draft> drafts = templateManager.updateTemplatesFromDrafts();
        templateManager.manageTemplates(drafts, configuration.getNumberOfFollowUps());
        LOGGER.info("Data synchronization completed");
    }

    private void simulateEmailProcessing() throws Exception {
        LOGGER.info("Starting dry run (email processing simulation)");
        List<Recipient> scheduledRecipients = retrieveAndPrepareRecipients();
        System.out.println("Potential Recipients for Email: " + scheduledRecipients.size());
        scheduledRecipients.forEach(recipient ->
                System.out.println("Recipient: " + recipient.getName() + " (ID: " + recipient.getId() + ")")
        );
        LOGGER.info("Dry run completed");
    }

    private void processEmails() throws Exception {
        LOGGER.info("Starting continuous email processing");

        // Track state of the last iteration
        boolean hadChanges = true;
        int consecutiveNoChangeIterations = 0;
        int MAX_NO_CHANGE_ITERATIONS = 3;

        while (hadChanges && consecutiveNoChangeIterations < MAX_NO_CHANGE_ITERATIONS) {
            try {
                hadChanges = false;

                // Flag to track if any changes occurred in this iteration
                boolean changesDetectedInIteration = false;

                // Retrieve and schedule recipients
                List<Recipient> scheduledRecipients = retrieveAndPrepareRecipients();

                if (!scheduledRecipients.isEmpty()) {
                    // Schedule emails for recipients
                    int scheduledEmailCount = emailService.scheduleEmailsForRecipients(scheduledRecipients);

                    if (scheduledEmailCount > 0) {
                        changesDetectedInIteration = true;
                        LOGGER.info("Emails scheduled for {} recipients", scheduledRecipients.size());
                    }
                }

                // Prepare and send pending emails
                List<Email> sentEmails = prepareAndSendPendingEmails();

                if (!sentEmails.isEmpty()) {
                    changesDetectedInIteration = true;
                    LOGGER.info("{} emails prepared and sent", sentEmails.size());
                }

                // Update the changes flag and iteration counter
                if (changesDetectedInIteration) {
                    hadChanges = true;
                    consecutiveNoChangeIterations = 0;
                } else {
                    consecutiveNoChangeIterations++;
                }

                Thread.sleep(2000); // Small delay to prevent overwhelming the system

            } catch (Exception e) {
                LOGGER.error("Error processing emails", e);
                if (isCriticalException(e)) {
                    throw e;
                }
            }
        }

        if (consecutiveNoChangeIterations >= MAX_NO_CHANGE_ITERATIONS) {
            LOGGER.warn("Reached maximum iterations without significant changes. Stopping processing.");
        }
    }

    private boolean isCriticalException(Exception e) {
        return e instanceof IOException ||
                e instanceof RecipientServiceException ||
                e instanceof RuntimeException;
    }

    private void resetGoogleCredentials() throws Exception {
        // Remove or reset Google-related credential files
        Path credentialsPath = Path.of("./tokens/StoredCredential");
        Files.deleteIfExists(credentialsPath);

        // If there are other credential or token files, delete them
        Path googleTokensDir = Path.of("./tokens");
        if (Files.exists(googleTokensDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(googleTokensDir)) {
                for (Path entry : stream) {
                    Files.delete(entry);
                }
            }
            Files.delete(googleTokensDir);
        }
        LOGGER.info("Google credentials and tokens reset");
    }

    private void clearDatabase() throws Exception {
        List<String> tables = List.of(
                "Emails",
                "Recipients",
                "Schedules",
                "Templates"
        );

        for (String table : tables) {
            databaseManager.dropTable(table);
            databaseManager.truncateTable(table);
        }
    }

    /**
     * Synchronizes data across different sources
     */
    private void synchronizeApplicationData() throws Exception {
        try {
            // Synchronize recipients from spreadsheet
            recipientSynchronizationService.syncRecipients();
            LOGGER.info("Recipients synchronized from spreadsheet");

            // Manage schedules
            scheduleService.manageSchedules(configuration.getNumberOfFollowUps());
            LOGGER.info("Schedules managed");

            emailService.retrieveAndSyncAlreadyScheduledAndSentEmails();
            LOGGER.info("Retrieved and synced all already scheduled and sent emails");

            List<Draft> drafts = templateManager.updateTemplatesFromDrafts();
            LOGGER.info("Email templates updated");

            templateManager.manageTemplates(drafts, configuration.getNumberOfFollowUps());
            LOGGER.info("Email templates loaded");
        } catch (Exception e) {
            LOGGER.error("Error synchronizing application data", e);
            throw e;
        }
    }

    /**
     * Retrieves and prepares recipients for email scheduling
     */
    private List<Recipient> retrieveAndPrepareRecipients() throws RecipientServiceException {
        List<Recipient> scheduledRecipients = recipientSynchronizationService
                .getRecipientsWithInitialEmailDate();

        return updateRecipientsEmailStatus(scheduledRecipients);
    }

    /**
     * Updates recipients' email status
     */
    private List<Recipient> updateRecipientsEmailStatus(List<Recipient> scheduledRecipients)
            throws RecipientServiceException {
        int index = 0;
        for (Recipient recipient : scheduledRecipients) {
            Optional<Email> initialEmail = emailService.getInitialEmailByRecipientId(recipient.getId());

            if (initialEmail.isPresent() && isEmailNotSentOrPending(initialEmail.get())) {
                Recipient updatedRecipient = recipientSynchronizationService
                        .updateInitialEmailDateIfPast(recipient);
                scheduledRecipients.set(index, updatedRecipient);
            }
            index++;
        }

        return recipientSynchronizationService.getRecipientsWithInitialEmailDate();
    }

    /**
     * Checks if email is not sent or pending
     */
    private boolean isEmailNotSentOrPending(Email email) {
        return email == null ||
                (!email.getStatus().equals(EmailStatus.SENT) && !email.getStatus().equals(EmailStatus.PENDING));
    }

    /**
     * Prepares and sends pending emails
     */
    private List<Email> prepareAndSendPendingEmails() throws Exception {
        List<Email> emails1 = emailService.getPendingEmailsReadyToSend();

        List<Email> emails = recipientSynchronizationService.prepareEmailsForSending(emails1);

        if (!emails.isEmpty()) {
            emailService.sendEmails(emails);
        }

        return emails;
    }

    public static void main(String[] args) {
        printLogo();

        try {
            Main mailSchedulerApp = new Main();
            mailSchedulerApp.run(args);
        } catch (Exception e) {
            LOGGER.error("Application encountered an error", e);
            e.printStackTrace();
            System.exit(1);
        }
    }


    private static void printLogo() {
        String logo = """
                 __    __          _   _   ____           _                  _           _              \s
                 |  \\/  |   __ _  (_) | | / ___|    ___  | |__     ___    __| |  _   _  | |   ___   _ __\s
                 | |\\/| |  / _` | | | | | \\___ \\   / __| | '_ \\   / _ \\  / _` | | | | | | |  / _ \\ | '__|
                 | |  | | | (_| | | | | |  ___) | | (__  | | | | |  __/ | (_| | | |_| | | | |  __/ | |  \s
                 |_|  |_|  \\__,_| |_| |_| |____/   \\___| |_| |_|  \\___|  \\__,_|  \\__,_| |_|  \\___| |_|
                """;
        System.out.println(logo);
    }
}
