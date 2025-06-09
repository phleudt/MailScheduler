package com.mailscheduler.domain.model.recipient;

import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.base.EntityId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing an email recipient in the system.
 * <p>
 *     A recipient is derived from a contact but includes additional information specific to email communications,
 *     such as salutation, reply status, and initial contact date. This entity is used for organizing and tracking
 *     email communications with contacts.
 * </p>
 */
public class Recipient extends IdentifiableEntity<Recipient> {
    private final EmailAddress emailAddress;
    private final String salutation;
    private final boolean hasReplied;
    private LocalDate initialContactDate;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder containing recipient values
     */
    private Recipient(Builder builder) {
        this.setId(builder.id);
        this.emailAddress = builder.emailAddress;
        this.salutation = builder.salutation;
        this.hasReplied = builder.hasReplied;
        this.initialContactDate = builder.initialContactDate;
    }

    /**
     * Gets the email address of the recipient.
     *
     * @return The email address
     */
     public EmailAddress getEmailAddress() {
         return emailAddress;
     }

    /**
     * Gets the salutation used for addressing the recipient.
     *
     * @return The salutation (may be null)
     */
     public String getSalutation() {
        return salutation;
    }

    public boolean hasReplied() {
        return hasReplied;
    }

    /**
     * Gets the date when the recipient was first contacted.
     *
     * @return The initial contact date (may be null if not yet contacted)
     */
    public LocalDate getInitialContactDate() {
        return initialContactDate;
    }

    /**
     * Sets the date when the recipient was first contacted.
     *
     * @param initialContactDate The initial contact date
     * @throws IllegalStateException if initial contact date is already set
     */
    public void setInitialContactDate(LocalDate initialContactDate) {
        if (this.initialContactDate != null) {
            throw new IllegalStateException("Initial contact date is already set and cannot be changed");
        }
        this.initialContactDate = initialContactDate;
    }

    /**
     * Checks if this recipient has been contacted previously.
     *
     * @return true if the recipient has been contacted before
     */
    public boolean hasBeenContacted() {
        return initialContactDate != null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipient that)) return false;
        if (!super.equals(o)) return false;

        if (!Objects.equals(emailAddress, that.emailAddress)) return false;
        if (!Objects.equals(salutation, that.salutation)) return false;
        if (!Objects.equals(hasReplied, that.hasReplied)) return false;
        return Objects.equals(initialContactDate, that.initialContactDate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0);
        result = 31 * result + (salutation != null ? salutation.hashCode() : 0);
        result = 31 * result + (hasReplied ? 1 : 0);
        result = 31 * result + (initialContactDate != null ? initialContactDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Recipient{" +
                "id=" + getId() +
                ", emailAddress=" + emailAddress +
                ", salutation='" + salutation + '\'' +
                ", hasReplied=" + hasReplied +
                ", initialContactDate=" + initialContactDate +
                '}';
    }

    /**
     * Builder for creating Recipient instances.
     */
    public static class Builder {
        private EntityId<Recipient> id;
        private EmailAddress emailAddress;
        private String salutation;
        private boolean hasReplied;
        private LocalDate initialContactDate;

        public Builder setId(EntityId<Recipient> id) {
            this.id = id;
            return this;
        }

        public Builder setEmailAddress(EmailAddress emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setSalutation(String salutation) {
            this.salutation = salutation;
            return this;
        }

        public Builder setHasReplied(boolean hasReplied) {
            this.hasReplied = hasReplied;
            return this;
        }

        public Builder setInitialContactDate(LocalDate initialContactDate) {
            this.initialContactDate = initialContactDate;
            return this;
        }

        /**
         * Initializes the builder with values from an existing Recipient.
         *
         * @param recipient The recipient to copy values from
         * @return This builder instance
         */
        public Builder from(Recipient recipient) {
            this.id = recipient.getId();
            this.emailAddress = recipient.getEmailAddress();
            this.salutation = recipient.getSalutation();
            this.hasReplied = recipient.hasReplied();
            this.initialContactDate = recipient.getInitialContactDate();
            return this;
        }

        public Recipient build() {
            return new Recipient(this);
        }
    }
}