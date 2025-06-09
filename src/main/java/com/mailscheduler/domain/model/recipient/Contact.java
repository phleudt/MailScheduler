package com.mailscheduler.domain.model.recipient;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;

import java.util.Objects;

/**
 * Entity representing a contact in the system.
 * <p>
 *     A contact contains essential information about a person or organization that may be contacted via email.
 *     Contacts are typically sourced from spreadsheets and serve as the basis for creating recipients.
 * </p>
 */
public class Contact extends IdentifiableEntity<Contact> {
    private final String name;
    private final String website;
    private final String phoneNumber;
    private final String sheetTitle;
    private final SpreadsheetReference spreadsheetRow;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder containing contact values
     */
    protected Contact(Builder builder) {
        this.setId(builder.id);
        this.name = builder.name;
        this.website = builder.website;
        this.phoneNumber = builder.phoneNumber;
        this.sheetTitle = builder.sheetTitle;
        this.spreadsheetRow = builder.spreadsheetRow;
    }

    public String getName() {
        return name;
    }

    public String getWebsite() {
        return website;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Gets the sheet title where this contact is stored.
     *
     * @return The sheet title
     */
    public String getSheetTitle() {
        return sheetTitle;
    }

    /**
     * Gets the spreadsheet row reference for this contact.
     *
     * @return The spreadsheet row reference
     */
    public SpreadsheetReference getSpreadsheetRow() {
        return spreadsheetRow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact that)) return false;
        if (!super.equals(o)) return false;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(website, that.website)) return false;
        if (!Objects.equals(phoneNumber, that.phoneNumber)) return false;
        if (!Objects.equals(sheetTitle, that.sheetTitle)) return false;
        return Objects.equals(spreadsheetRow, that.spreadsheetRow);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (website != null ? website.hashCode() : 0);
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        result = 31 * result + (sheetTitle != null ? sheetTitle.hashCode() : 0);
        result = 31 * result + (spreadsheetRow != null ? spreadsheetRow.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", website='" + website + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", sheetTitle='" + sheetTitle + '\'' +
                ", spreadsheetRow=" + spreadsheetRow +
                '}';
    }

    /**
     * Builder for creating Contact instances.
     */
    public static class Builder {
        private EntityId<Contact> id;
        private String name;
        private String website;
        private String phoneNumber;
        private String sheetTitle;
        private SpreadsheetReference spreadsheetRow;

        public Builder setId(EntityId<Contact> id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setWebsite(String website) {
            this.website = website;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setSheetTitle(String sheetTitle) {
            this.sheetTitle = sheetTitle;
            return this;
        }

        public Builder setSpreadsheetRow(int row) {
            if (row <= 0) {
                throw new IllegalArgumentException("Row number must be greater than zero");
            }
            this.spreadsheetRow = SpreadsheetReference.ofRow(row);
            return this;
        }

        /**
         * Sets the spreadsheet row reference for this contact.
         * @param spreadsheetRow The reference must be a row type reference
         * @throws IllegalArgumentException if reference is not null and not a row type
         */
        public Builder setSpreadsheetRow(SpreadsheetReference spreadsheetRow) {
            if (spreadsheetRow != null && spreadsheetRow.getType() != SpreadsheetReference.ReferenceType.ROW) {
                throw new IllegalArgumentException("Spreadsheet reference must be a row reference");
            }
            this.spreadsheetRow = spreadsheetRow;
            return this;
        }

        /**
         * Initializes the builder with values from an existing Contact.
         *
         * @param contact The contact to copy values from
         * @return This builder instance
         */
        public Builder from(Contact contact) {
            Objects.requireNonNull(contact, "Contact cannot be null");

            this.id = contact.getId();
            this.name = contact.getName();
            this.website = contact.getWebsite();
            this.phoneNumber = contact.getPhoneNumber();
            this.sheetTitle = contact.getSheetTitle();
            this.spreadsheetRow = contact.getSpreadsheetRow();
            return this;
        }

        public Contact build() {
            return new Contact(this);
        }
    }
}
