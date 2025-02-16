package com.mailscheduler.model;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.util.TimeUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Contact {
    private String name;
    private String emailAddress;
    private String salutation;
    private String domain;
    private String phoneNumber;
    private ZonedDateTime initialEmailDate;
    private boolean hasReplied;
    private int spreadsheetRow;

    private Contact(Builder builder) {
        this.name = builder.name;
        this.emailAddress = builder.emailAddress;
        this.salutation = builder.salutation;
        this.domain = builder.domain;
        this.phoneNumber = builder.phoneNumber;
        this.initialEmailDate = builder.initialEmailDate;
        this.hasReplied = builder.hasReplied;
        this.spreadsheetRow = builder.spreadsheetRow;
    }

    public static List<Contact> buildContactsFromColumns(List<ValueRange> valueRanges, int rowCount) {
        List<Contact> contacts = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            List<String> rowValues = getRowFromColumns(valueRanges, index);
            if (!isEmptyRow(rowValues)) {
                int rowIndex = getRowIndexFromValueRange(valueRanges, index);
                contacts.add(buildContactFromList(rowValues, rowIndex));
            }
        }
        return contacts;
    }

    private static int getRowIndexFromValueRange(List<ValueRange> valueRanges, int index) {
        ValueRange valueRange = valueRanges.get(0);
        if (valueRange == null) {
            throw new IllegalArgumentException("ValueRange cannot be null");
        }
        return getStartingRow(valueRange.getRange()) + index;
    }

    private static int getStartingRow(String spreadsheetRange) {
        if (spreadsheetRange == null) {
            throw new IllegalArgumentException("googleRange cannot be null");
        }
        // Format for string: Tabellenblatt1!A2:A5
        String[] parts = spreadsheetRange.split("!");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid range format");
        }

        String[] cells = parts[1].split(":");
        if (cells.length == 0) {
            throw new IllegalArgumentException("Invalid cell range format");
        }

        String startCell = cells[0];
        String rowNumber = startCell.replaceAll("[^0-9]", "");
        return Integer.parseInt(rowNumber);
    }

    private static List<String> getRowFromColumns(List<ValueRange> valueRanges, int rowIndex) {
        List<String> rowValues = new ArrayList<>(valueRanges.size());
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> column = valueRange.getValues();
            if (rowIndex < column.size() && !column.get(rowIndex).isEmpty()) {
                rowValues.add(column.get(rowIndex).get(0).toString());
            } else {
                rowValues.add(null);
            }
        }
        return rowValues;
    }

    private static Contact buildContactFromList(List<String> attributes, int spreadsheetRow) {
        Contact.Builder builder = new Contact.Builder();

        builder.setDomain(getOrNull(attributes, 0))
                .setEmailAddress(getOrNull(attributes, 1))
                .setName(getOrNull(attributes, 2))
                .setSalutation(getOrNull(attributes, 3))
                .setPhoneNumber(getOrNull(attributes, 4))
                .setInitialEmailDate(TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)))
                .setSpreadsheetRow(spreadsheetRow)
                .setIsSendingCriteriaFulfilled(getSendingCriteria(attributes));

        return builder.build();
    }

    private static boolean getSendingCriteria(List<String> attributes) {
        for (int i = 6; i < attributes.size(); i++) {
            if (getOrNull(attributes, i) == null) {
                return false;
            }
        }
        return true;
    }

    private static String getOrNull(List<String> list, int index) {
        return (index < list.size() && list.get(index) != null) ? list.get(index) : null;
    }

    private static boolean isEmptyRow(List<String> rowValues) {
        return rowValues.stream().allMatch(val -> val == null || val.isEmpty());
    }

    // Getter and Setter
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public ZonedDateTime getInitialEmailDate() {
        return initialEmailDate;
    }

    public void setInitialEmailDate(ZonedDateTime initialEmailDate) {
        this.initialEmailDate = initialEmailDate;
    }

    public boolean hasReplied() {
        return hasReplied;
    }

    public void setHasReplied(boolean hasReplied) {
        this.hasReplied = hasReplied;
    }

    public int getSpreadsheetRow() {
        return spreadsheetRow;
    }

    public void setSpreadsheetRow(int spreadsheetRow) {
        this.spreadsheetRow = spreadsheetRow;
    }

    @Override
    public String toString() {
        return "Contact{domain=" + domain + ", email=" + emailAddress + ", name=" + name + ", phone number=" + phoneNumber + "}";
    }

    public static class Builder {
        private String name;
        private String emailAddress;
        private String salutation;
        private String domain;
        private String phoneNumber;
        private ZonedDateTime initialEmailDate;
        private boolean hasReplied;
        private int spreadsheetRow;
        public boolean isSendingCriteriaFulfilled;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setSalutation(String salutation) {
            this.salutation = salutation;
            return this;
        }

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setInitialEmailDate(ZonedDateTime initialEmailDate) {
            this.initialEmailDate = initialEmailDate;
            return this;
        }

        public Builder setHasReplied(boolean hasReplied) {
            this.hasReplied = hasReplied;
            return this;
        }

        public Builder setSpreadsheetRow(int spreadsheetRow) {
            this.spreadsheetRow = spreadsheetRow;
            return this;
        }

        public Builder setIsSendingCriteriaFulfilled(boolean sendingCriteriaFulfilled) {
            this.isSendingCriteriaFulfilled = sendingCriteriaFulfilled;
            return this;
        }

        public Contact build() {
            if (isSendingCriteriaFulfilled) {
                if (initialEmailDate == null) {
                    initialEmailDate = ZonedDateTime.now();
                }
            } else {
                initialEmailDate = null;
            }

            return new Contact(this);
        }
    }
}
