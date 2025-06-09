package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.common.vo.ThreadId;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.RecipientRepository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.RecipientEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL implementation of the RecipientRepository.
 * Handles persistence of Recipient entities in a relational database.
 */
public class RecipientSqlRepository extends AbstractSqlRepository<Recipient, RecipientMetadata, RecipientEntity>
        implements RecipientRepository {

    public RecipientSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "recipients";
    }

    @Override
    protected RecipientEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new RecipientEntity(
                rs.getLong("id"),
                rs.getLong("contact_id"),
                rs.getString("email_address"),
                rs.getLong("followup_plan_id"),
                rs.getString("salutation"),
                rs.getTimestamp("initial_contact_date"),
                rs.getBoolean("has_replied"),
                rs.getString("thread_id")
        );
    }

    @Override
    protected RecipientEntity toTableEntity(Recipient domainEntity, RecipientMetadata metadata) {
        return new RecipientEntity(
                domainEntity.getId() != null ? domainEntity.getId().value() : null,
                metadata.contactId().value(),
                domainEntity.getEmailAddress().value(),
                metadata.followupPlanId().value(),
                domainEntity.getSalutation(),
                domainEntity.getInitialContactDate() != null ? Timestamp.valueOf(domainEntity.getInitialContactDate().atStartOfDay()) : null,
                domainEntity.hasReplied(),
                metadata.threadId() != null ? metadata.threadId().value() : null
        );
    }

    @Override
    protected Recipient toDomainEntity(RecipientEntity tableEntity) {
        return new Recipient.Builder()
                .setId(EntityId.of(tableEntity.getId()))
                .setEmailAddress(EmailAddress.of(tableEntity.getEmailAddress()))
                .setSalutation(tableEntity.getSalutation())
                .setHasReplied(tableEntity.hasReplied())
                .setInitialContactDate(
                        tableEntity.getInitialContactDate() != null ? tableEntity.getInitialContactDate().toLocalDateTime().toLocalDate() : null
                )
                .build();
    }

    @Override
    protected RecipientMetadata toMetadata(RecipientEntity tableEntity) {
        return new RecipientMetadata(
                EntityId.of(tableEntity.getContactId()),
                EntityId.of(tableEntity.getFollowupPlanId()),
                new ThreadId(tableEntity.getThreadId())
        );
    }

    @Override
    protected String createInsertSql() {
        return String.format(
                """
                INSERT INTO %s (
                    contact_id, email_address, followup_plan_id, salutation,
                    initial_contact_date, has_replied, thread_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """, tableName());
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
                """
                UPDATE %s SET
                    contact_id = ?,
                    email_address = ?,
                    followup_plan_id = ?,
                    salutation = ?,
                    initial_contact_date = ?,
                    has_replied = ?,
                    thread_id = ?
                WHERE id = ?
                RETURNING *
                """, tableName());
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, RecipientEntity entity) throws SQLException {
        stmt.setLong(1, entity.getContactId());
        stmt.setString(2, entity.getEmailAddress());
        stmt.setLong(3, entity.getFollowupPlanId());
        stmt.setString(4, entity.getSalutation());
        stmt.setTimestamp(5, entity.getInitialContactDate());
        stmt.setBoolean(6, entity.hasReplied());
        stmt.setString(7, entity.getThreadId());

        // For updates, we need to set the ID as the 7th parameter
        if (entity.getId() != null) {
            stmt.setLong(8, entity.getId());
        }
    }

    @Override
    public List<EntityData<Recipient, RecipientMetadata>> findByContactId(EntityId<Contact> contactId) {
        String sql = String.format("SELECT * FROM %s WHERE contact_id = ?", tableName());
        List<EntityData<Recipient, RecipientMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, contactId.value());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecipientEntity entity = mapResultSetToEntity(rs);
                    result.add(EntityData.of(toDomainEntity(entity), toMetadata(entity)));
                }
            }
        } catch (SQLException e) {
        }
        return result;
    }

    @Override
    public List<EntityData<Recipient, RecipientMetadata>> findByFollowUpPlanId(EntityId<com.mailscheduler.domain.model.schedule.FollowUpPlan> planId) {
        String sql = "SELECT * FROM " + tableName() + " WHERE followup_plan_id = ?";
        List<EntityData<Recipient, RecipientMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, planId.value());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecipientEntity entity = mapResultSetToEntity(rs);
                    result.add(EntityData.of(toDomainEntity(entity), toMetadata(entity)));
                }
            }
        } catch (SQLException e) {
        }
        return result;
    }

    @Override
    public List<EntityData<Recipient, RecipientMetadata>> findByHasReplied(boolean hasReplied) {
        String sql = "SELECT * FROM " + tableName() + " WHERE has_replied = ?";
        List<EntityData<Recipient, RecipientMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, hasReplied);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecipientEntity entity = mapResultSetToEntity(rs);
                    result.add(EntityData.of(toDomainEntity(entity), toMetadata(entity)));
                }
            }
        } catch (SQLException e) {
        }
        return result;
    }

    @Override
    public List<EntityData<Recipient, RecipientMetadata>> findRecipientsNeedingInitialContact() {
        String sql = "SELECT * FROM " + tableName() + " WHERE has_replied = false AND initial_contact_date IS NULL";
        List<EntityData<Recipient, RecipientMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RecipientEntity entity = mapResultSetToEntity(rs);
                result.add(EntityData.of(toDomainEntity(entity), toMetadata(entity)));
            }
        } catch (SQLException e) {
        }
        return result;
    }
}
