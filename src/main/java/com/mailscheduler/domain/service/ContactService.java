package com.mailscheduler.domain.service;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.repository.ContactRepository;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for contact-related operations.
 */
public class ContactService {
    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * Finds all contacts from the given sheet that match the specified rows.
     *
     * @param sheetTitle title of the sheet to filter by
     * @param rowNumbers set of row numbers to filter by
     * @return list of matching contacts
     */
    public List<Contact> findContactsBySheetAndRows(String sheetTitle, Set<Integer> rowNumbers) {
        List<EntityData<Contact, NoMetadata>> allContacts = contactRepository.findAllWithMetadata();

        return allContacts.stream()
                .map(EntityData::entity)
                .filter(contact -> sheetTitle.equals(contact.getSheetTitle()) &&
                        rowNumbers.contains(contact.getSpreadsheetRow().extractRowNumber()))
                .collect(Collectors.toList());
    }

    /**
     * Finds a contact by its ID.
     *
     * @param contactId ID of the contact to find
     * @return the contact if found
     */
    public Optional<Contact> findById(EntityId<Contact> contactId) {
        return contactRepository.findByIdWithMetadata(contactId)
                .map(EntityData::entity);
    }

    /**
     * Finds all contacts.
     *
     * @return list of all contacts
     */
    public List<Contact> findAll() {
        return contactRepository.findAllWithMetadata().stream()
                .map(EntityData::entity)
                .collect(Collectors.toList());
    }
}