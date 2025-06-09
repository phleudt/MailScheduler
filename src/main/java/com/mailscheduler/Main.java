package com.mailscheduler;

import com.mailscheduler.application.email.EmailService;
import com.mailscheduler.application.email.scheduling.EmailScheduler;
import com.mailscheduler.application.email.scheduling.PlaceholderResolver;
import com.mailscheduler.application.email.sending.gateway.EmailGateway;
import com.mailscheduler.application.email.sending.gateway.GmailAdapter;
import com.mailscheduler.application.email.service.EmailOrchestrationService;
import com.mailscheduler.application.email.service.EmailSchedulingService;
import com.mailscheduler.application.email.service.EmailSendingService;
import com.mailscheduler.application.recipient.RecipientService;
import com.mailscheduler.application.synchronization.spreadsheet.SpreadsheetSynchronizationService;
import com.mailscheduler.application.synchronization.SynchronizationCoordinator;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.GoogleSheetAdapter;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.application.synchronization.template.TemplateSyncStrategy;
import com.mailscheduler.application.synchronization.template.gateway.GmailDraftAdapter;
import com.mailscheduler.domain.factory.SpreadsheetConfigurationFactory;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.application.service.PlanService;
import com.mailscheduler.domain.repository.*;
import com.mailscheduler.domain.service.ContactService;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.infrastructure.google.sheet.GoogleSheetService;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.database.schema.TableDefinitions;
import com.mailscheduler.infrastructure.persistence.repository.*;
import com.mailscheduler.infrastructure.service.FollowUpManagementService;
import com.mailscheduler.infrastructure.spreadsheet.SpreadsheetService;

import java.sql.SQLException;

public class Main {
    private String spreadsheetId = "";

    public Main() {

    }

    private void clearDatabase() throws SQLException {
        DatabaseFacade databaseFacade = new DatabaseFacade();
        String[] tables = TableDefinitions.TABLE_NAMES;
        for (String table : tables) {
            databaseFacade.dropTable(table);
        }
        databaseFacade.initialize();
    }

    private void synchronizeApplicationData() throws Exception {
        SpreadsheetGateway spreadsheetGateway = new GoogleSheetAdapter(GoogleSheetService.getInstance());
        DatabaseFacade db = new DatabaseFacade();
        ConfigurationRepository configurationRepository = new ConfigurationSqlRepository(db);

        // Test if spreadsheet id is already defined in config table, if not use defined id
        try {
            spreadsheetId = configurationRepository.getActiveConfiguration().getSpreadsheetId();
        } catch (Exception e) {
            if (spreadsheetId.isEmpty()) {
                System.out.println("No Spreadsheet ID found");
                return;
            }
        }

        SpreadsheetSynchronizationService spreadsheetSynchronizationService = new SpreadsheetSynchronizationService(
                spreadsheetGateway,
                new ContactSqlRepository(db),
                new RecipientSqlRepository(db),
                new EmailSqlRepository(db),
                configurationRepository,
                new TemplateSqlRepository(db),
                new FollowUpManagementService(
                        new FollowUpPlanSqlRepository(db),
                        new FollowUpStepSqlRepository(db)
                )
        );

        SynchronizationCoordinator synchronizationCoordinator = getSynchronizationCoordinator(spreadsheetGateway, spreadsheetSynchronizationService, db);

        synchronizationCoordinator.synchronizeAll();
    }

    private SynchronizationCoordinator getSynchronizationCoordinator(SpreadsheetGateway spreadsheetGateway, SpreadsheetSynchronizationService spreadsheetSynchronizationService, DatabaseFacade db) throws Exception {
        SpreadsheetConfigurationFactory configurationFactory = new SpreadsheetConfigurationFactory(spreadsheetGateway);
        SpreadsheetConfiguration spreadsheetConfiguration =
                configurationFactory.createFromExistingSpreadsheet(spreadsheetId);

        return new SynchronizationCoordinator(
                spreadsheetSynchronizationService,
                new TemplateSyncStrategy(
                        new GmailDraftAdapter(GmailService.getInstance()),
                        new TemplateSqlRepository(db)
                ),
                spreadsheetConfiguration
        );
    }

    private void sendEmails() throws Exception {
        // Initialize dependencies
        DatabaseFacade db = new DatabaseFacade();
        RecipientRepository recipientRepository = new RecipientSqlRepository(db);
        ContactRepository contactRepository = new ContactSqlRepository(db);
        EmailRepository emailRepository = new EmailSqlRepository(db);
        EmailGateway emailGateway = new GmailAdapter(GmailService.getInstance());
        ConfigurationRepository configRepository = new ConfigurationSqlRepository(db);
        SpreadsheetGateway spreadsheetGateway = new GoogleSheetAdapter(GoogleSheetService.getInstance());

        RecipientService recipientService = new RecipientService(
                recipientRepository,
                contactRepository,
                spreadsheetGateway
        );

        // Get application configuration
        ApplicationConfiguration appConfig = configRepository.getActiveConfiguration();

        // Create email service
        EmailService emailService = new EmailService(
                new EmailSendingService(emailGateway, emailRepository),
                new EmailSchedulingService(
                        new EmailScheduler(appConfig.getSenderEmailAddress(), emailRepository,
                                new PlaceholderResolver(spreadsheetGateway, contactRepository, recipientRepository, appConfig.getSpreadsheetId())),
                        emailRepository),
                recipientService
        );

        // Create email orchestration service
        EmailOrchestrationService orchestrationService = new EmailOrchestrationService(
                emailService,
                configRepository,
                new SpreadsheetService(spreadsheetGateway, new SpreadsheetConfigurationFactory(spreadsheetGateway)),
                new RecipientService(recipientRepository,
                        contactRepository,
                        spreadsheetGateway),
                new ContactService(contactRepository),
                new PlanService(new FollowUpManagementService(
                        new FollowUpPlanSqlRepository(db),
                        new FollowUpStepSqlRepository(db)),
                        new TemplateSqlRepository(db)
                )
        );

        // Process and send emails
        orchestrationService.processPendingEmailsAndSend(appConfig.getSaveMode());
    }


    public static void main(String[] args) {
        try {
            Main main = new Main();

            // main.clearDatabase();

            main.synchronizeApplicationData();
            main.sendEmails();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
