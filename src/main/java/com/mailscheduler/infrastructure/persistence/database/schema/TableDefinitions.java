package com.mailscheduler.infrastructure.persistence.database.schema;

import java.util.List;

/**
 * Defines the database schema tables and provides SQL statements for creating them.
 * Contains the structure of all database tables used in the application.
 */
public final class TableDefinitions {

    private TableDefinitions() {
        // Private constructor to prevent instantiation
    }

    /**
     * All table names used in this application and defined in this class.
     */
    public static String[] TABLE_NAMES = new String[]{
            "contacts",
            "recipients",
            "emails",
            "followup_plan_steps",
            "followup_plans",
            "templates",
            "column_mappings",
            "configuration"
    };

    /**
     * Table definition for storing contact information.
     */
    public static final String CONTACTS_TABLE = """
            -- Stores basic contact information
            CREATE TABLE IF NOT EXISTS contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                website TEXT,
                phone_number TEXT,
                sheet_title TEXT,
                spreadsheet_row INTEGER NOT NULL CHECK (spreadsheet_row > 0),
                UNIQUE (sheet_title, spreadsheet_row)
            );
            """;

    /**
     * Table definition for storing recipient information.
     */
    public static final String RECIPIENTS_TABLE = """
            -- Represents a specific instance of a contact being targeted for communication, potentially as part of a plan
            CREATE TABLE IF NOT EXISTS recipients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                contact_id INTEGER NOT NULL,
                email_address TEXT NOT NULL,
                followup_plan_id INTEGER, -- Which follow-up plan (if any) is assigned to this recipient
                salutation TEXT,
                initial_contact_date TIMESTAMP,
                has_replied BOOLEAN DEFAULT FALSE NOT NULL,
                thread_id TEXT,
                UNIQUE (contact_id, email_address)
                FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE,
                FOREIGN KEY (followup_plan_id) REFERENCES followup_plans(id) ON DELETE SET NULL
            );
            """;

    /**
     * Table definition for storing email templates.
     */
    public static final String TEMPLATE_TABLE = """
            -- Defines reusable email templates
            CREATE TABLE IF NOT EXISTS templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                template_type TEXT CHECK (template_type IN ('INITIAL', 'FOLLOW_UP', 'DEFAULT_INITIAL', 'DEFAULT_FOLLOW_UP')) NOT NULL,
                subject_template TEXT NOT NULL,
                body_template TEXT NOT NULL,
                delimiters TEXT,
                placeholders TEXT,
                draft_id TEXT
            );
            """;

    /**
     * Table definition for storing follow-up plan structures.
     */
    public static final String FOLLOW_UP_PLANS_TABLE = """
            -- Defines follow-up plan structures
            CREATE TABLE IF NOT EXISTS followup_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                followup_plan_type TEXT CHECK (followup_plan_type IN ('DEFAULT_FOLLOW_UP_PLAN', 'FOLLOW_UP_PLAN')) DEFAULT 'FOLLOW_UP_PLAN'
            );
            """;

    /**
     * Table definition for storing follow-up plan steps.
     */
    public static final String FOLLOW_UP_PLAN_STEPS_TABLE = """
            -- Defines the individual steps within a follow-up plan
            CREATE TABLE IF NOT EXISTS followup_plan_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                plan_id INTEGER NOT NULL,
                step_number INTEGER NOT NULL CHECK (step_number >= 0),
                waiting_period INTEGER NOT NULL CHECK (waiting_period >= 0), -- Days to wait after the previous step/initial email before sending this
                template_id INTEGER,
                UNIQUE (plan_id, step_number),
                FOREIGN KEY (plan_id) REFERENCES followup_plans(id) ON DELETE CASCADE,
                FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE RESTRICT
            );
            """;

    /**
     * Table definition for storing email information.
     */
    public static final String EMAIL_TABLE = """
            -- Represents a single email entity (potentially sent to multiple recipients)
            CREATE TABLE IF NOT EXISTS emails (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                initial_email_id INTEGER, -- Link follow-ups back to the first email in the sequence
                recipient_id INTEGER,
                subject TEXT,
                body TEXT,
                email_type TEXT CHECK (email_type IN ('INITIAL', 'FOLLOW_UP', 'EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')) NOT NULL,
                followup_number INTEGER DEFAULT 0 NOT NULL CHECK (followup_number >= 0) , -- Which step in the sequence this email represents (0 for initial)
                status TEXT CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')) DEFAULT 'PENDING',
                failure_reason TEXT DEFAULT NULL,  -- Store error message if status is 'FAILED'
                scheduled_date TIMESTAMP,
                sent_date TIMESTAMP,
                FOREIGN KEY (initial_email_id) REFERENCES emails(id) ON DELETE CASCADE
                FOREIGN KEY (recipient_id) REFERENCES recipients(id) ON DELETE CASCADE
            );
            """;

    /**
     * Table definition for storing application configuration.
     */
    public static final String CONFIGURATION_TABLE = """
            CREATE TABLE IF NOT EXISTS configuration (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                spreadsheet_id TEXT NOT NULL,
                sender_email TEXT NOT NULL,
                save_mode BOOLEAN NOT NULL DEFAULT true,
                sending_criteria_column TEXT,
                UNIQUE (spreadsheet_id)
            )
            """;

    /**
     * Table definition for storing column mappings between the database and spreadsheets.
     */
    public static final String COLUMN_MAPPINGS = """
            CREATE TABLE IF NOT EXISTS column_mappings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                config_id INTEGER NOT NULL,
                type TEXT CHECK (type IN ('CONTACT', 'EMAIL', 'RECIPIENT')) NOT NULL,
                column_name TEXT,
                column_reference TEXT
            )
            """;


    /**
     * Gets all table creation statements in the order they should be executed.
     * This ensures that tables with foreign keys are created after the tables they reference.
     *
     * @return list of table creation SQL statements in dependency order
     */
    public static List<String> getAllTableStatements() {
        // Add tables in order of dependency
        return List.of(
                CONTACTS_TABLE,
                FOLLOW_UP_PLANS_TABLE,
                TEMPLATE_TABLE,
                RECIPIENTS_TABLE,
                FOLLOW_UP_PLAN_STEPS_TABLE,
                EMAIL_TABLE,
                CONFIGURATION_TABLE,
                COLUMN_MAPPINGS
        );
    }
}