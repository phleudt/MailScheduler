package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.repository.EmailRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.EmailEntity;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL implementation of the EmailRepository interface.
 * Handles persistence of Email entities and their metadata in a relational database.
 */
public class EmailSqlRepository extends AbstractSqlRepository<Email, EmailMetadata, EmailEntity> implements EmailRepository {

    public EmailSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    public String tableName() {
        return "emails";
    }

    @Override
    protected EmailEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new EmailEntity(
                rs.getLong("id"),
                rs.getLong("initial_email_id"),
                rs.getLong("recipient_id"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getString("email_type"),
                rs.getInt("followup_number"),
                rs.getString("status"),
                rs.getString("failure_reason"),
                rs.getTimestamp("scheduled_date"),
                rs.getTimestamp("sent_date")
        );
    }

    @Override
    protected EmailEntity toTableEntity(Email email, EmailMetadata metadata) {
        return new EmailEntity(
                email.getId() != null ? email.getId().value() : null,
                metadata.initialEmailId() != null ? metadata.initialEmailId().value() : null,
                metadata.recipientId() != null ? metadata.recipientId().value() : null,
                email.getSubject() != null ? email.getSubject().value() : null,
                email.getBody() != null ? email.getBody().value() : null,
                email.getType().toString(),
                metadata.followupNumber(),
                metadata.status().toString(),
                metadata.failureReason(),
                metadata.scheduledDate() != null ? Timestamp.valueOf(metadata.scheduledDate().atStartOfDay()) : null,
                metadata.sentDate() != null ? Timestamp.valueOf(metadata.sentDate().atStartOfDay()) : null
        );
    }
    @Override
    protected Email toDomainEntity(EmailEntity tableEntity) {
        return new Email(
                EntityId.of(tableEntity.getId()),
                null,
                null,
                Subject.of(tableEntity.getSubject()),
                Body.of(tableEntity.getBody()),
                EmailType.valueOf(tableEntity.getEmailType())
        );
    }

    @Override
    protected EmailMetadata toMetadata(EmailEntity entity) {
        return new EmailMetadata.Builder()
                .initialEmailId(entity.getInitialEmailId() != null ? EntityId.of(entity.getInitialEmailId()) : null)
                .recipientId(entity.getRecipientId() != null ? EntityId.of(entity.getRecipientId()) : null)
                .followupNumber(entity.getFollowupNumber())
                .status(EmailStatus.valueOf(entity.getStatus()))
                .failureReason(entity.getFailureReason())
                .scheduledDate(entity.getScheduledDate() != null ? entity.getScheduledDate().toLocalDateTime().toLocalDate() : null)
                .sentDate(entity.getSentDate() != null ? entity.getSentDate().toLocalDateTime().toLocalDate() : null)
                .build();
    }

    @Override
    protected String createInsertSql() {
        return String.format(
            """
            INSERT INTO %s (
                initial_email_id, recipient_id, subject, body, email_type,
                followup_number, status, failure_reason,
                scheduled_date, sent_date
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """, tableName());
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
            """
            UPDATE %s SET
                initial_email_id = ?,
                recipient_id = ?,
                subject = ?,
                body = ?,
                email_type = ?,
                followup_number = ?,
                status = ?,
                failure_reason = ?,
                scheduled_date = ?,
                sent_date = ?
            WHERE id = ?
            RETURNING *
            """, tableName());
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, EmailEntity entity) throws SQLException {
        if (entity.getInitialEmailId() != null) {
            stmt.setLong(1, entity.getInitialEmailId());
        } else {
            stmt.setNull(1, Types.BIGINT);
        }

        if (entity.getRecipientId() != null) {
            stmt.setLong(2, entity.getRecipientId());
        } else {
            stmt.setNull(2, Types.BIGINT);
        }
        stmt.setString(3, entity.getSubject());
        stmt.setString(4, entity.getBody());
        stmt.setString(5, entity.getEmailType());
        stmt.setInt(6, entity.getFollowupNumber());
        stmt.setString(7, entity.getStatus());
        stmt.setString(8, entity.getFailureReason());
        stmt.setTimestamp(9, entity.getScheduledDate());
        stmt.setTimestamp(10, entity.getSentDate());

        // For updates, we need to set the ID as the 10th parameter
        if (entity.getId() != null) {
            stmt.setLong(11, entity.getId());
        }
    }

    // --- EmailRepository custom methods ---

    @Override
    public List<EntityData<Email, EmailMetadata>> findPendingScheduledBefore(LocalDate cutoff) {
        String sql = String.format("SELECT * FROM %s WHERE status = ? AND scheduled_date <= ?", tableName());
        List<EntityData<Email, EmailMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, EmailStatus.PENDING.toString());
            stmt.setTimestamp(2, Timestamp.valueOf(cutoff.atStartOfDay()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EmailEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public List<EntityData<Email, EmailMetadata>> findByStatus(EmailStatus status) {
        String sql = String.format("SELECT * FROM %s WHERE status = ?", tableName());
        List<EntityData<Email, EmailMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EmailEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public List<EntityData<Email, EmailMetadata>> findByType(EmailType type) {
        String sql = String.format("SELECT * FROM %s WHERE email_type = ?", tableName());
        List<EntityData<Email, EmailMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EmailEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public List<EntityData<Email, EmailMetadata>> findByRecipientId(EntityId<Recipient> recipientId) {
        String sql = String.format("SELECT * FROM %s WHERE recipient_id = ?", tableName());
        List<EntityData<Email, EmailMetadata>> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientId.value());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EmailEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public Optional<EntityData<Email, EmailMetadata>> findInitialEmailByRecipientId(EntityId<Recipient> recipientId) {
        String sql = String.format("SELECT * FROM %s  " +
                "WHERE recipient_id = ? AND e.followup_number = 0 ", tableName());
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientId.value());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EmailEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return Optional.empty();
    }
}
