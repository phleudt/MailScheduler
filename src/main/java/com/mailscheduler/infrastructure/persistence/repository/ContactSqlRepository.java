package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.repository.ContactRepository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.ContactEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SQL implementation of the ContactRepository.
 * Handles persistence of Contact entities in a relational database.
 */
public class ContactSqlRepository extends AbstractSqlRepository<Contact, NoMetadata, ContactEntity>
        implements ContactRepository {

    public ContactSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "contacts";
    }

    @Override
    protected ContactEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new ContactEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("website"),
                rs.getString("phone_number"),
                rs.getString("sheet_title"),
                rs.getInt("spreadsheet_row")
        );
    }

    @Override
    protected ContactEntity toTableEntity(Contact domainEntity, NoMetadata noMetadata) {
        return new ContactEntity(
                domainEntity.getId() != null ? domainEntity.getId().value() : null,
                domainEntity.getName(),
                domainEntity.getWebsite(),
                domainEntity.getPhoneNumber(),
                domainEntity.getSheetTitle(),
                domainEntity.getSpreadsheetRow().extractRowNumber()
        );
    }

    @Override
    protected Contact toDomainEntity(ContactEntity tableEntity) {
        return new Contact.Builder()
                .setId(EntityId.of(tableEntity.getId()))
                .setName(tableEntity.getName())
                .setWebsite(tableEntity.getWebsite())
                .setPhoneNumber(tableEntity.getPhoneNumber())
                .setSpreadsheetRow(tableEntity.getSpreadsheetRow())
                .setSheetTitle(tableEntity.getSheetTitle())
                .build();
    }

    @Override
    protected NoMetadata toMetadata(ContactEntity tableEntity) {
        return NoMetadata.getInstance();
    }

    @Override
    protected String createInsertSql() {
        return String.format(
                "INSERT INTO %s (name, website, phone_number, sheet_title, spreadsheet_row) VALUES (?, ?, ?, ?, ?) RETURNING *",
                tableName());
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
                " UPDATE %s SET name = ?, website = ?, phone_number = ?, sheet_title = ?, spreadsheet_row = ? WHERE id = ? RETURNING * ",
                tableName());
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, ContactEntity entity) throws SQLException {
        stmt.setString(1, entity.getName());
        stmt.setString(2, entity.getWebsite());
        stmt.setString(3, entity.getPhoneNumber());
        stmt.setString(4, entity.getSheetTitle());
        stmt.setInt(5, entity.getSpreadsheetRow());

        // For updates, we need to set the ID as the 5th parameter
        if (entity.getId() != null) {
            stmt.setLong(6, entity.getId());
        }
    }

    @Override
    public Optional<Contact> findBySpreadsheetRow(int rowNumber) {
        String sql = String.format("SELECT * FROM %s WHERE spreadsheet_row = ?", tableName());

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rowNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ContactEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(toDomainEntity(entity));
                }
            }
        } catch (SQLException e) {
            // Handle exception as needed (e.g., log)
        }
        return Optional.empty();
    }

    public void save(Contact contact) {
        saveWithMetadata(contact, NoMetadata.getInstance());
    }
}







