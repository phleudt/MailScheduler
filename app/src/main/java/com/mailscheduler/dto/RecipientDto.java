package com.mailscheduler.dto;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.SimpleTimeZone;

public final class RecipientDto {
    private final int id;
    private final String name;
    private final String emailAddress;
    private final String gender;
    private final String domain;
    private final String phoneNumber;
    private final ZonedDateTime initialEmailDate;
    private final boolean hasReplied;
    private final int spreadsheetRow;

    public RecipientDto(int id,
                        String name,
                        String emailAddress,
                        String gender,
                        String domain,
                        String phoneNumber,
                        ZonedDateTime initialEmailDate,
                        boolean hasReplied,
                        int spreadsheetRow
    ) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
        this.gender = gender;
        this.domain = domain;
        this.phoneNumber = phoneNumber;
        this.initialEmailDate = initialEmailDate;
        this.hasReplied = hasReplied;
        this.spreadsheetRow = spreadsheetRow;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getGender() {
        return gender;
    }

    public String getDomain() {
        return domain;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public ZonedDateTime getInitialEmailDate() {
        return initialEmailDate;
    }

    public String getInitialEmailDateAsString() {
        return initialEmailDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public boolean hasReplied() {
        return hasReplied;
    }

    public int getSpreadsheetRow() {
        return spreadsheetRow;
    }

    public RecipientDto withUpdatedInitialEmailDate(ZonedDateTime newInitialEmailDate) {
        return new RecipientDto(this.id,
                this.name,
                this.emailAddress,
                this.gender,
                this.domain,
                this.phoneNumber,
                newInitialEmailDate,
                hasReplied,
                spreadsheetRow
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipientDto that = (RecipientDto) o;
        return hasReplied == that.hasReplied &&
                spreadsheetRow == that.spreadsheetRow &&
                Objects.equals(name, that.name) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(phoneNumber, that.phoneNumber);
                // Objects.equals(initialEmailDate, that.initialEmailDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, emailAddress, domain, phoneNumber, initialEmailDate, hasReplied, spreadsheetRow);
    }

    @Override
    public String toString() {
        return "RecipientDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", domain='" + domain + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", initialEmailDate=" + initialEmailDate +
                ", hasReplied=" + hasReplied +
                ", spreadsheetRow=" + spreadsheetRow +
                '}';
    }

    public static class Builder {
        private int id;
        private String name;
        private String emailAddress;
        private String gender;
        private String domain;
        private String phoneNumber;
        private ZonedDateTime initialEmailDate;
        private boolean hasReplied;
        private int spreadsheetRow;

        public Builder() {
        }

        public Builder fromExisting(RecipientDto existingDto) {
            this.id = existingDto.getId();
            this.name = existingDto.getName();
            this.emailAddress = existingDto.getEmailAddress();
            this.gender = existingDto.getGender();
            this.domain = existingDto.getDomain();
            this.phoneNumber = existingDto.getPhoneNumber();
            this.initialEmailDate = existingDto.getInitialEmailDate();
            this.hasReplied = existingDto.hasReplied();
            this.spreadsheetRow = existingDto.getSpreadsheetRow();
            return this;
        }

        public Builder mergeWith(RecipientDto newDto) {
            if (newDto.getEmailAddress() != null && !newDto.getEmailAddress().isEmpty()) {
                this.emailAddress = newDto.getEmailAddress();
            }
            if (newDto.getName() != null && !newDto.getName().isEmpty()) {
                this.name = newDto.getName();
            }
            if (newDto.getDomain() != null && !newDto.getDomain().isEmpty()) {
                this.domain = newDto.getDomain();
            }
            if (newDto.getPhoneNumber() != null && !newDto.getPhoneNumber().isEmpty()) {
                this.phoneNumber = newDto.getPhoneNumber();
            }
            if (newDto.getInitialEmailDate() != null) {
                this.initialEmailDate = newDto.getInitialEmailDate();
            }
            this.hasReplied = newDto.hasReplied() || hasReplied;
            this.spreadsheetRow = newDto.getSpreadsheetRow();
            return this;
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setGender(String gender) {
            this.gender = gender;
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

        public RecipientDto build() {
            return new RecipientDto(id, name, emailAddress, gender, domain, phoneNumber, initialEmailDate, hasReplied, spreadsheetRow);
        }
    }

}
