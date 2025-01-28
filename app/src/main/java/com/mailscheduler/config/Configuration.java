package com.mailscheduler.config;

import com.mailscheduler.model.SpreadsheetReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Configuration {
    private final Map<String, SpreadsheetReference> contactColumns;
    private final Map<String, SpreadsheetReference> markEmailColumns;
    private final Map<String, SpreadsheetReference> markSchedulesForEmailColumns;
    private final String spreadsheetId;
    private final boolean saveMode;
    private final int numberOfFollowUps;
    private final List<SendingCriterion> sendingCriteria;
    private final String defaultSender;

    public Configuration(
            Map<String, SpreadsheetReference> contactColumns,
            Map<String, SpreadsheetReference> markEmailColumns,
            Map<String, SpreadsheetReference> markSchedulesForEmailColumns,
            String spreadsheetId,
            boolean saveMode,
            int numberOfFollowUps,
            List<SendingCriterion> sendingCriteria,
            String defaultSender
    ) {
        this.contactColumns = contactColumns;
        this.markEmailColumns = markEmailColumns;
        this.markSchedulesForEmailColumns = markSchedulesForEmailColumns;
        this.spreadsheetId = spreadsheetId;
        this.saveMode = saveMode;
        this.numberOfFollowUps = numberOfFollowUps;
        this.sendingCriteria = sendingCriteria;
        this.defaultSender = defaultSender;
    }

    public Map<String, SpreadsheetReference> getContactColumns() {
        return contactColumns;
    }

    public Map<String, SpreadsheetReference> getMarkEmailColumns() {
        return markEmailColumns;
    }

    public Map<String, SpreadsheetReference> getMarkSchedulesForEmailColumns() {
        return markSchedulesForEmailColumns;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public boolean isSaveMode() {
        return saveMode;
    }

    public int getNumberOfFollowUps() {
        return numberOfFollowUps;
    }

    public List<SendingCriterion> getSendingCriteria() {
        return sendingCriteria;
    }

    public void addSendingCriteria(SendingCriterion criteria) {
        sendingCriteria.add(criteria);
    }

    public void removeSendingCriteria(SendingCriterion criteria) {
        sendingCriteria.remove(criteria);
    }

    public String getDefaultSender() {
        return defaultSender;
    }

    public static class Builder {
        private Map<String, SpreadsheetReference> contactColumns;
        private Map<String, SpreadsheetReference> markEmailColumns;
        private Map<String, SpreadsheetReference> markSchedulesForEmailColumns;
        private String spreadsheetId;
        private boolean saveMode = true;
        private int numberOfFollowUps = 0;
        private List<SendingCriterion> sendingCriteria = new ArrayList<>();
        private String defaultSender;

        public Builder contactColumns(Map<String, SpreadsheetReference> contactColumns) {
            this.contactColumns = contactColumns;
            return this;
        }

        public Builder markEmailColumns(Map<String, SpreadsheetReference> markEmailColumns) {
            this.markEmailColumns = markEmailColumns;
            return this;
        }

        public Builder markSchedulesForEmailColumns(Map<String, SpreadsheetReference> markSchedulesForEmailColumns) {
            this.markSchedulesForEmailColumns = markSchedulesForEmailColumns;
            return this;
        }

        public Builder spreadsheetId(String spreadsheetId) {
            this.spreadsheetId = spreadsheetId;
            return this;
        }

        public Builder saveMode(boolean saveMode) {
            this.saveMode = saveMode;
            return this;
        }

        public Builder numberOfFollowUps(int numberOfFollowUps) {
            this.numberOfFollowUps = numberOfFollowUps;
            return this;
        }

        public Builder setSendingCriteria(List<SendingCriterion> criteria) {
            this.sendingCriteria = criteria;
            return this;
        }

        public Builder addSendingCriterion(SendingCriterion criterion) {
            this.sendingCriteria.add(criterion);
            return this;
        }

        public Builder setDefaultSender(String defaultSender) {
            this.defaultSender = defaultSender;
            return this;
        }

        public Configuration build() {
            return new Configuration(
                    contactColumns,
                    markEmailColumns,
                    markSchedulesForEmailColumns,
                    spreadsheetId,
                    saveMode,
                    numberOfFollowUps,
                    sendingCriteria,
                    defaultSender
            );
        }
    }
}