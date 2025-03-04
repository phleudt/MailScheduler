package com.mailscheduler.domain.recipient;

import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Recipient {
    private final RecipientId id;
    private final FullName name;
    private final EmailAddress emailAddress;
    private final String salutation;
    private final String domain;
    private final String phoneNumber;
    private final SpreadsheetReference spreadsheetRow;
    private final ZonedDateTime initialEmailDate;
    private final boolean hasReplied;

    private Recipient(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.emailAddress = builder.emailAddress;
        this.salutation = builder.salutation;
        this.domain = builder.domain;
        this.phoneNumber = builder.phoneNumber;
        this.spreadsheetRow = builder.spreadsheetRow;
        this.initialEmailDate = builder.initialEmailDate;
        this.hasReplied = builder.hasReplied;
    }

    // Getters
    public RecipientId getId() {
        return id;
    }

    public FullName getName() {
        return name;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    public String getSalutation() {
        return salutation;
    }

    public String getDomain() {
        return domain;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public SpreadsheetReference getSpreadsheetRow() {
        return spreadsheetRow;
    }

    public boolean hasReplied() {
        return hasReplied;
    }

    public ZonedDateTime getInitialEmailDate() {
        return initialEmailDate;
    }

    /**
     * Returns the initial email date formatted as a string
     *
     * @return The formatted date string or empty string if the date is null
     */
    public String getInitialEmailDateAsString() {
        if (initialEmailDate == null) return "";
        return initialEmailDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

    }

    /**
     * Creates a new Recipient with an updated initial email date
     *
     * @param newDate The new date to set
     * @return A new Recipient instance with the updated date
     */
    public Recipient withUpdatedInitialEmailDate(ZonedDateTime newDate) {
        return new Builder()
                .fromExisting(this)
                .setInitialEmailDate(newDate)
                .setPreserveInitialEmailDate(true)
                .build();
    }

    public boolean equalsWithoutInitialEmailDate(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipient that = (Recipient) o;
        return hasReplied == that.hasReplied &&
                spreadsheetRow == that.spreadsheetRow &&
                Objects.equals(name, that.name) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipient that = (Recipient) o;
        return hasReplied == that.hasReplied &&
                spreadsheetRow.equals(that.spreadsheetRow) &&
                Objects.equals(name, that.name) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(initialEmailDate, that.initialEmailDate);
    }

    @Override
    public String toString() {
        return "Recipient{domain=" + domain + ", email=" + emailAddress + ", name=" + name + ", phone number=" + phoneNumber + "}";
    }

    public static class Builder {
        private RecipientId id;
        private FullName name;
        private EmailAddress emailAddress;
        private String salutation;
        private String domain;
        private String phoneNumber;
        private SpreadsheetReference spreadsheetRow;
        private ZonedDateTime initialEmailDate;
        private boolean hasReplied;
        public boolean isSendingCriteriaFulfilled;
        public boolean preserveInitialEmailDate;

        public Builder setId(int id) {
            this.id = RecipientId.of(id);
            return this;
        }

        public Builder setName(String name) throws IllegalArgumentException {
            this.name = FullName.of(name);
            return this;
        }

        public Builder setEmailAddress(String emailAddress) throws IllegalArgumentException {
            this.emailAddress = EmailAddress.of(emailAddress);
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

        public Builder setSpreadsheetRow(Integer rowNumber) {
            this.spreadsheetRow = rowNumber != null ? SpreadsheetReference.ofRow(rowNumber) : null;
            return this;
        }

        public Builder setSpreadsheetReference(SpreadsheetReference reference) {
            if (reference != null && reference.getType() != SpreadsheetReference.ReferenceType.ROW) {
                throw new IllegalArgumentException("Spreadsheet reference must be a row reference");
            }
            this.spreadsheetRow = reference;
            return this;
        }

        public Builder setIsSendingCriteriaFulfilled(boolean sendingCriteriaFulfilled) {
            this.isSendingCriteriaFulfilled = sendingCriteriaFulfilled;
            return this;
        }

        /**
         * Flag to preserve the explicitly set initialEmailDate
         * regardless of sending criteria status
         *
         * @param preserve Whether to preserve the date
         * @return This builder instance
         */
        public Builder setPreserveInitialEmailDate(boolean preserve) {
            this.preserveInitialEmailDate = preserve;
            return this;
        }

        public Builder fromExisting(Recipient recipient) {
            this.id = recipient.getId();
            this.name = recipient.getName();
            this.emailAddress = recipient.getEmailAddress();
            this.salutation = recipient.getSalutation();
            this.domain = recipient.getDomain();
            this.phoneNumber = recipient.getPhoneNumber();
            this.spreadsheetRow = recipient.getSpreadsheetRow();
            this.initialEmailDate = recipient.getInitialEmailDate();
            this.hasReplied = recipient.hasReplied();
            return this;
        }

        public Recipient build() {
            if (!preserveInitialEmailDate) {
                if (isSendingCriteriaFulfilled) {
                    if (initialEmailDate == null) {
                        initialEmailDate = ZonedDateTime.now();
                    }
                } else {
                    initialEmailDate = null;
                }
            }

            return new Recipient(this);
        }
    }
}
