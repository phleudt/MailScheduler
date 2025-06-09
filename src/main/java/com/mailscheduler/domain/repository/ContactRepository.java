package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.recipient.Contact;

import java.util.Optional;

/**
 * Repository interface for managing Contact entities.
 * <p>
 *     Contacts represent individuals whose information is stored in the system, typically imported from a
 *     spreadsheet before becoming active email recipients.
 * </p>
 */
public interface ContactRepository extends Repository<Contact, NoMetadata> {
    /**
     * Finds contact originating from a specific spreadsheet row number.
     *
     * @param rowNumber The spreadsheet row number used during import/creation.
     * @return Optional contact associated with that row number.
     */
    Optional<Contact> findBySpreadsheetRow(int rowNumber);

    /**
     * Saves (creates or updates) a contact.
     *
     * @param contact The contact to save
     */
    void save(Contact contact);

}
